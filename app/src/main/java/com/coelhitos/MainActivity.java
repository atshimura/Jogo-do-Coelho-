package com.coelhitos;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.content.pm.ActivityInfo;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class MainActivity extends Activity {
    private GameView game;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(5894 | 1024 | 512 | 4096);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        game = new GameView(this);
        setContentView(game);
    }
    @Override protected void onPause() { super.onPause(); if (game != null) game.pause(); }
    @Override protected void onResume() { super.onResume(); if (game != null) game.resume(); }
    @Override protected void onDestroy() { if (game != null) game.dispose(); super.onDestroy(); }
}

final class GameView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Bitmap rabbitIdle, rabbitHop;
    private final Bitmap enemySmallSprite, enemyLargeSprite, spikeSprite, pitSprite, fireSprite;
    private final Random rng = new Random(44);
    private final AudioEngine audio = new AudioEngine();
    private final ArrayList<Pickup> pickups = new ArrayList<>();
    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<Hazard> hazards = new ArrayList<>();
    private final ArrayList<Shot> shots = new ArrayList<>();
    private final ArrayList<Shot> enemyShots = new ArrayList<>();
    private final ArrayList<Burst> bursts = new ArrayList<>();
    private final ArrayList<Decor> decor = new ArrayList<>();
    private float w, h, ground, unit, levelLength, camera, playerX, playerY, vy;
    private float normalSpeed, bunnyH, bunnyW, hitCooldown, powerTime, animTime, lastFrame, jumpForwardSpeed;
    private float stepTimer;
    private float portalEntryTime, fallTime, fallStartX, fallStartY, fallTargetX;
    private int hearts = 3, carrots = 0, power = 0, state = 0, collectedChests = 0;
    private int levelNumber = 1, selectedLevel = 1;
    private boolean onGround = true, leftHeld, rightHeld, upHeld, jumpHeld, actionHeld, facingRight = true, restartPending;
    private int stickPointer = -1, actionPointer = -1, jumpPointer = -1;
    private float stickX, stickY;
    private long actionDownAt;
    private float jumpBufferTime, queuedJumpDirection, lastShotDirection;
    private boolean stickJumpWasDown;
    private long startMillis;
    private final RectF stickArea = new RectF();
    private String notice = "";
    private float noticeTime;

    private static final int NONE=0, THORN=1, LIGHTNING=2, FEATHER=3, FIRE=4, SUPER_JUMP=5;

    GameView(Context context) {
        super(context);
        rabbitIdle=BitmapFactory.decodeResource(context.getResources(),R.drawable.coelhitos_rabbit_idle);
        rabbitHop=BitmapFactory.decodeResource(context.getResources(),R.drawable.coelhitos_rabbit_hop);
        enemySmallSprite=loadGameSprite(R.drawable.coelhitos_enemy_small);
        enemyLargeSprite=loadGameSprite(R.drawable.coelhitos_enemy_large);
        spikeSprite=loadGameSprite(R.drawable.coelhitos_hazard_spikes);
        pitSprite=loadGameSprite(R.drawable.coelhitos_hazard_pit);
        fireSprite=loadGameSprite(R.drawable.coelhitos_hazard_fire);
        p.setFilterBitmap(true);
        setFocusable(true);
        audio.start();
    }

    private Bitmap loadGameSprite(int resourceId) {
        BitmapFactory.Options options=new BitmapFactory.Options();
        options.inSampleSize=4;
        return BitmapFactory.decodeResource(getResources(),resourceId,options);
    }

    void pause() { audio.pause(); }
    void resume() { if (state != 2) audio.start(); }
    void dispose() { audio.stop(); }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        w = getWidth(); h = getHeight();
        if (w <= 0 || h <= 0) return;
        unit = h / 360f;
        bunnyH = h * .245f;
        // Silhueta lateral: o personagem tem corpo comprido e fica de quatro.
        float idleAspect=rabbitIdle==null?1.12f:rabbitIdle.getWidth()/(float)rabbitIdle.getHeight();
        float hopAspect=rabbitHop==null?idleAspect:rabbitHop.getWidth()/(float)rabbitHop.getHeight();
        bunnyW = bunnyH * Math.max(idleAspect,hopAspect);
        normalSpeed = h * .38f;
        levelLength = normalSpeed * 180f;
        ground = terrainY(playerX);
        float now = System.nanoTime() / 1_000_000_000f;
        float dt = lastFrame == 0 ? .016f : Math.min(.04f, now-lastFrame);
        lastFrame = now;
        animTime += dt;
        if (state == 1 || state == 3 || state == 4) update(dt);
        drawWorld(c);
        if (state == 0) drawTitle(c);
        else drawHud(c);
        if(state==1) drawControls(c);
        if (noticeTime > 0) {
            noticeTime -= dt;
            drawNotice(c);
        }
        postInvalidateDelayed(16);
    }

    private void resetLevel() {
        pickups.clear(); enemies.clear(); hazards.clear(); shots.clear(); enemyShots.clear(); bursts.clear(); decor.clear();
        hearts=3; carrots=0; power=NONE; powerTime=0; collectedChests=0; restartPending=false; fallTime=0;
        playerX = h*.65f; ground=terrainY(playerX); playerY = ground-bunnyH; vy=0; jumpForwardSpeed=0; onGround=true; facingRight=true;
        camera=0; hitCooldown=0; jumpBufferTime=0;queuedJumpDirection=0;leftHeld=rightHeld=upHeld=jumpHeld=actionHeld=false;
        stickPointer=actionPointer=jumpPointer=-1; stickJumpWasDown=false;
        if(levelNumber==2) { populateCaveLevel(); startMillis=System.currentTimeMillis(); return; }
        for(int i=0;i<5;i++) pickups.add(new Pickup(playerX+65*unit + i*112*unit, 0, 0));
        for(int i=0;i<5;i++) pickups.add(new Pickup(levelLength*(.12f+i*.17f), 0, 0));
        // Mais cenouras distribuídas pela rota, sem substituir as cinco do início.
        float[] extraCarrots={.035f,.16f,.225f,.285f,.35f,.41f,.475f,.535f,.60f,.665f,.73f,.795f,.86f,.93f};
        for(float fraction:extraCarrots) pickups.add(new Pickup(levelLength*fraction,0,0));
        for(int i=0;i<3;i++) pickups.add(new Pickup(levelLength*(.24f+i*.25f), 1, 0));
        for(int i=0;i<5;i++) {
            float x=levelLength*(.1f+i*.19f);
            pickups.add(new Pickup(x, 2, 0));
        }
        for(int i=0;i<22;i++) {
            float x=levelLength*(.045f+i*.043f);
            boolean big=i%5==0 || i%7==3;
            int type=i%7==3?Enemy.HORNED:(i%5==2?Enemy.TAIL:Enemy.GROUND);
            enemies.add(new Enemy(x,big?3:1,big,36*unit+(i%4)*14*unit,(big?68:49)*unit,type));
        }
        // Aves posicionadas em corredores livres entre obstáculos para não
        // bloquear o mesmo salto necessário para superar um perigo do chão.
        float[] birdFractions={.096f,.222f,.348f,.474f,.600f,.726f,.852f,.945f};
        for(int i=0;i<birdFractions.length;i++) {
            float x=levelLength*birdFractions[i];
            boolean big=i==3;
            enemies.add(new Enemy(x,big?3:1,big,38*unit+(i%3)*8*unit,(big?60:49)*unit,Enemy.FLYING));
        }
        for(int i=0;i<14;i++) {
            float x=levelLength*(.065f+i*.063f);
            // A fase 1 não tem buracos; eles ficam disponíveis para fases futuras.
            int type = i%2==0 ? 0 : 2;
            hazards.add(new Hazard(x, type, (54+ (i%3)*14)*unit));
        }
        for(int i=0;i<150;i++) {
            float x=(i*236f + (i%5)*53f)*unit;
            decor.add(new Decor(x, i%6, 0.65f+(i%4)*.13f));
        }
        startMillis=System.currentTimeMillis();
    }

    private void beginGame() {
        levelNumber=selectedLevel;
        resetLevel();
        state=1;
        audio.setCaveTheme(levelNumber==2);
        audio.start();
    }

    private void returnToTitle() {
        selectedLevel=levelNumber;
        state=0;
        leftHeld=rightHeld=upHeld=jumpHeld=actionHeld=false;
        stickPointer=actionPointer=jumpPointer=-1;
        stickJumpWasDown=false;
        audio.setCaveTheme(selectedLevel==2);
        audio.start();
    }

    private void selectLevel(int level) {
        selectedLevel=level==2?2:1;
        audio.setCaveTheme(selectedLevel==2);
        audio.start();
    }

    private boolean isMenuCard(float x,float y,int level) {
        float top=h*.085f+184*unit, bottom=h*.085f+238*unit;
        if(y<top||y>bottom)return false;
        if(level==1)return x>=w*.105f+25*unit&&x<w*.5f-8*unit;
        return x>w*.5f+8*unit&&x<=w*.895f-25*unit;
    }

    private boolean isPlayButton(float x,float y) {
        float top=h*.085f+249*unit,bottom=h*.085f+294*unit;
        return x>=w*.36f&&x<=w*.64f&&y>=top&&y<=bottom;
    }

    private boolean isHomeButton(float x,float y) {
        return x>=w-91*unit&&x<=w-8*unit&&y>=h*.18f&&y<=h*.31f;
    }

    private void populateCaveLevel() {
        for(int i=0;i<10;i++) pickups.add(new Pickup(levelLength*(.075f+i*.087f),0,0));
        for(int i=0;i<3;i++) pickups.add(new Pickup(levelLength*(.22f+i*.27f),1,0));
        for(int i=0;i<5;i++) pickups.add(new Pickup(levelLength*(.12f+i*.17f),2,0));
        for(int i=0;i<18;i++) {
            float x=levelLength*(.045f+i*.051f);
            int type=i%3==0?Enemy.PLANT_SHOOTER:(i%3==1?Enemy.BEETLE:Enemy.GROUND);
            int hp=type==Enemy.BEETLE && i%2==0?3:1;
            enemies.add(new Enemy(x,hp,hp>1,32*unit+(i%4)*9*unit,(hp>1?69:55)*unit,type));
        }
        for(int i=0;i<8;i++) {
            float x=levelLength*(.13f+i*.105f);
            enemies.add(new Enemy(x,2,false,42*unit+(i%3)*8*unit,56*unit,Enemy.MOTH));
        }
        float[] pits={.205f,.385f,.565f,.755f,.905f};
        for(int i=0;i<pits.length;i++) hazards.add(new Hazard(levelLength*pits[i],1,(66+(i%2)*18)*unit));
        for(int i=0;i<13;i++) {
            float x=levelLength*(.08f+i*.067f);
            if(isPitAt(x)) continue;
            hazards.add(new Hazard(x,i%2==0?0:2,(52+(i%3)*12)*unit));
        }
        for(int i=0;i<150;i++) {
            float x=(i*236f+(i%5)*53f)*unit;
            decor.add(new Decor(x,6,0.7f+(i%4)*.15f));
        }
    }

    private void advanceToCave() {
        int savedHearts=hearts,savedCarrots=carrots,savedPower=power;
        float savedPowerTime=powerTime;
        levelNumber=2; selectedLevel=2; resetLevel();
        hearts=savedHearts; carrots=savedCarrots; power=savedPower; powerTime=savedPowerTime;
        state=1; audio.setCaveTheme(true); audio.start(); say("Caverna dos Cristais");
    }

    private void update(float dt) {
        if(state==3) { updatePortalEntry(dt); return; }
        if(state==4) { updateFall(dt); return; }
        if (hitCooldown>0) hitCooldown-=dt;
        if (power==LIGHTNING || power==SUPER_JUMP) {
            powerTime-=dt;
            if(powerTime<=0) { boolean wasLightning=power==LIGHTNING; power=NONE; powerTime=0; say(wasLightning?"O Raio acabou":"O Supersalto acabou"); }
        }
        float dir = (rightHeld?1:0) - (leftHeld?1:0);
        float playerXBeforeMove=playerX;
        if (Math.abs(dir)>.1f) facingRight=dir>0;
        boolean running = actionHeld && Math.abs(dir)>.1f;
        float speed = normalSpeed*(running?1.7f:1f)*(power==LIGHTNING?1.55f:1f);
        if(!onGround && jumpForwardSpeed!=0) speed=Math.max(speed,Math.abs(jumpForwardSpeed));
        if (Math.abs(dir)>.1f) playerX += dir*speed*dt;
        else if (!onGround && jumpForwardSpeed!=0) playerX += jumpForwardSpeed*dt;
        if(Math.abs(dir)>.1f && onGround) {
            stepTimer-=dt;
            if(stepTimer<=0) { audio.effect(running?15:14); stepTimer=running?.34f:.48f; }
        } else stepTimer=0;
        playerX=Math.max(0,Math.min(levelLength-bunnyW*.6f,playerX));
        if (!onGround) {
            vy += h*3.7f*dt;
            playerY += vy*dt;
            float landingY=terrainY(playerX);
            if (playerY+bunnyH>=landingY && !isPitAt(playerX+bunnyW*.52f)) { ground=landingY; playerY=ground-bunnyH; vy=0; onGround=true; jumpForwardSpeed=0; }
        } else { ground=terrainY(playerX); playerY=ground-bunnyH; }
        if(!onGround && isPitAt(playerX+bunnyW*.52f) && playerY+bunnyH>terrainY(playerX)+h*.045f) {
            onGround=true;beginFall();return;
        }
        if(onGround && isPitAt(playerX+bunnyW*.52f)) { beginFall(); return; }
        if (jumpBufferTime > 0) {
            jumpBufferTime = Math.max(0, jumpBufferTime-dt);
            if (onGround) launchJump(queuedJumpDirection);
        }
        float target=playerX-w*.28f;
        camera=Math.max(0,Math.min(levelLength-w,target));

        Iterator<Enemy> ei=enemies.iterator();
        while(ei.hasNext()) {
            Enemy e=ei.next();
            if(e.dead) continue;
            float pace=e.flying?1.15f:1.7f;
            e.x += Math.sin(animTime*pace+e.phase)*e.range*dt;
            if(levelNumber==2 && e.type==Enemy.PLANT_SHOOTER) {
                e.attackTime-=dt;
                float dx=playerX-e.x;
                if(e.attackTime<=0 && Math.abs(dx)<w*.82f && Math.abs(dx)>bunnyW*1.6f) {
                    float shotDir=dx<0?-1:1;
                    float sx=e.x+shotDir*e.size*.82f;
                    float sy=enemyFeetY(e)-e.size*.68f;
                    enemyShots.add(new Shot(sx,sy,shotDir*220*unit,0,1,5,0));
                    e.attackTime=2.4f+(e.phase%1.1f);
                    audio.effect(17);
                }
            }
            if(power==LIGHTNING && enemyOverlapsPlayer(e,playerX,playerY)) {
                damageEnemy(e,3); audio.effect(5);
            }
        }
        resolvePlayerCollisions(playerXBeforeMove,dir);
        // Reiniciar limpa as listas; só faça isso depois que os iteradores terminarem.
        if (restartPending) { restartAfterDamage(); return; }
        Iterator<Pickup> pi=pickups.iterator();
        while(pi.hasNext()) {
            Pickup q=pi.next();
            float pickupGround=terrainY(q.x);
            float py=q.type==2 ? pickupGround-bunnyH*.65f : pickupGround-bunnyH*.25f;
            if(Math.abs(q.x-(playerX+bunnyW*.45f))<bunnyW*.75f && Math.abs(py-(playerY+bunnyH*.45f))<bunnyH) {
                if(q.type==0) { carrots++; audio.effect(0); say("Cenoura +1"); }
                else if(q.type==1) { hearts=Math.min(3,hearts+1); audio.effect(1); say("Coração!"); }
                else { power=randomPower(); collectedChests++; audio.effect(2); say(powerName(power)); powerTime=powerDuration(power); }
                pi.remove();
            }
        }
        if (restartPending) { restartAfterDamage(); return; }
        Iterator<Shot> si=shots.iterator();
        while(si.hasNext()) {
            Shot s=si.next(); s.x+=s.vx*dt; s.y+=s.vy*dt; s.age+=dt;
            boolean hit=false;
            for(Enemy e:enemies) if(!e.dead && Math.abs(e.x-s.x)<e.size*.55f && Math.abs((terrainY(e.x)-e.size*.85f)-s.y)<e.size*.7f) {
                damageEnemy(e,s.damage); hit=true;
                if(s.kind==3) {
                    bursts.add(new Burst(s.x,s.y));
                    for(Enemy nearby:enemies) if(!nearby.dead && nearby!=e && Math.abs(nearby.x-s.x)<78*unit) damageEnemy(nearby,1);
                    audio.effect(4);
                } else if(s.kind==2) audio.effect(4); else audio.effect(3);
                break;
            }
            if(hit || s.age>(s.kind==4 ? .65f : 2.5f) || s.x<camera-80*unit || s.x>camera+w+120*unit) si.remove();
        }
        updateEnemyShots(dt);
        if(restartPending) { restartAfterDamage(); return; }
        Iterator<Burst> bi=bursts.iterator();
        while(bi.hasNext()) { Burst b=bi.next(); b.age+=dt; if(b.age>.32f) bi.remove(); }
        float portalCenter=levelLength-w*.08f;
        if(dir>0 && playerX+bunnyW*.5f>=portalCenter-bunnyW*.9f) beginPortalEntry();
    }

    private void updateEnemyShots(float dt) {
        Iterator<Shot> hostile=enemyShots.iterator();
        while(hostile.hasNext()) {
            Shot s=hostile.next();s.x+=s.vx*dt;s.y+=s.vy*dt;s.age+=dt;
            if(overlaps(playerX+bunnyW*.16f,playerY+bunnyH*.20f,playerX+bunnyW*.84f,playerY+bunnyH*.91f,
                    s.x-9*unit,s.y-8*unit,s.x+9*unit,s.y+8*unit)) {
                hurt();hostile.remove();
            } else if(s.age>3f||s.x<camera-50*unit||s.x>camera+w+50*unit) hostile.remove();
        }
    }

    private boolean isPitAt(float worldX) {
        if(levelNumber!=2)return false;
        for(Hazard z:hazards) if(z.type==1 && Math.abs(worldX-z.x)<z.size*.47f)return true;
        return false;
    }

    private void beginFall() {
        if(state!=1||!onGround)return;
        Hazard pit=null;
        float center=playerX+bunnyW*.52f;
        for(Hazard z:hazards)if(z.type==1&&Math.abs(center-z.x)<z.size*.47f){pit=z;break;}
        if(pit==null)return;
        state=4;fallTime=0;fallStartX=playerX;fallStartY=playerY;
        float safeEdge=pit.x-pit.size*.58f-bunnyW*.82f;
        fallTargetX=Math.max(0,safeEdge);
        leftHeld=rightHeld=upHeld=jumpHeld=actionHeld=false;
        stickPointer=actionPointer=jumpPointer=-1;jumpBufferTime=0;queuedJumpDirection=0;
        onGround=false;vy=0;jumpForwardSpeed=0;audio.effect(18);
    }

    private void updateFall(float dt) {
        if(state!=4)return;
        fallTime+=dt;
        vy+=h*4.5f*dt;
        playerY+=vy*dt;
        // A short tumble and shrink make the fall readable before the safe return.
        if(fallTime>=.82f) {
            hearts--;audio.lifeLost();hitCooldown=1.15f;
            if(hearts<=0){restartPending=true;restartAfterDamage();return;}
            playerX=fallTargetX;ground=terrainY(playerX);playerY=ground-bunnyH;
            onGround=true;vy=0;fallTime=0;state=1;camera=Math.max(0,Math.min(levelLength-w,playerX-w*.28f));
            say("Cuidado com o buraco!");
        }
    }

    private void beginPortalEntry() {
        state=3; portalEntryTime=0;
        facingRight=true; leftHeld=rightHeld=upHeld=jumpHeld=actionHeld=false;
        stickPointer=actionPointer=jumpPointer=-1; jumpBufferTime=0; queuedJumpDirection=0;
        onGround=true; vy=0; jumpForwardSpeed=0;
    }

    private void updatePortalEntry(float dt) {
        portalEntryTime+=dt;
        facingRight=true; rightHeld=true; onGround=true; vy=0;
        float portalCenter=levelLength-w*.08f;
        float targetX=portalCenter-bunnyW*.5f;
        playerX=Math.min(targetX,playerX+normalSpeed*.62f*dt);
        ground=terrainY(playerX); playerY=ground-bunnyH;
        camera=Math.max(0,Math.min(levelLength-w,playerX-w*.28f));
        if(playerX>=targetX || portalEntryTime>=2.4f) {
            playerX=targetX; ground=terrainY(playerX); playerY=ground-bunnyH;
            rightHeld=false; audio.effect(7);
            if(levelNumber==1) advanceToCave();
            else { state=2; audio.stopMusic(); say("Você entrou no portal!"); }
        }
    }

    private int randomPower() {
        return powerForRoll(rng.nextInt(100));
    }
    private int powerForRoll(int n) {
        if(n<25)return THORN;
        if(n<45)return FIRE;
        if(n<70)return FEATHER;
        if(n<83)return LIGHTNING;
        return SUPER_JUMP;
    }
    private float powerDuration(int pwr) { return pwr==LIGHTNING?8f:pwr==SUPER_JUMP?20f:0f; }
    private String powerName(int pwr) { return pwr==THORN?"Poder Espinho!":pwr==LIGHTNING?"Poder Raio!":pwr==FEATHER?"Poder Pena!":pwr==SUPER_JUMP?"Supersalto!":"Poder Fogo!"; }
    private float horizontalDirection() { return rightHeld&&!leftHeld?1:leftHeld&&!rightHeld?-1:0; }
    private void requestJump(String source) {
        queuedJumpDirection=horizontalDirection();
        if (queuedJumpDirection!=0) facingRight=queuedJumpDirection>0;
        jumpBufferTime=.22f;
        if(onGround) launchJump(queuedJumpDirection,source);
    }
    private void launchJump(float dir) { launchJump(dir,"buffer"); }
    private void launchJump(float dir,String source) {
        if(!onGround)return;
        // Um toque gera um salto. O buffer curto evita perder o comando se o
        // botão for apertado pouco antes de tocar o chão.
        jumpBufferTime=0;
        if(dir!=0) facingRight=dir>0;
        float jumpForwardMultiplier=power==SUPER_JUMP?1.85f:1.55f;
        jumpForwardSpeed=normalSpeed*jumpForwardMultiplier*dir;
        Log.d("CoelhitosInput","jump dir="+(int)dir+" source="+source);
        vy=-jumpImpulse(dir); onGround=false; audio.effect(6);
    }
    private float jumpImpulse(float dir) { return power==SUPER_JUMP?h*2f:(dir==0?h*1.47f:h*1.8f); }
    private void say(String s) { notice=s; noticeTime=1.25f; }
    private void hurt() {
        if(hitCooldown>0 || state!=1)return;
        hitCooldown=1.1f;
        if(power!=NONE) { audio.effect(8); power=NONE; powerTime=0; say("Você perdeu o poder"); }
        else {
            hearts--; audio.lifeLost(); say("Cuidado!");
            if(hearts<=0) restartPending=true;
        }
    }
    private void restartAfterDamage() { restartPending=false; resetLevel(); state=1; audio.start(); say("A fase recomeçou"); }
    private void damageEnemy(Enemy e,int amount) { if(e.dead)return; e.hp-=amount; if(e.hp<=0){e.dead=true; audio.effect(5);} }

    private float enemyFeetY(Enemy e) {
        float floor=terrainY(e.x);
        return e.flying?floor-h*.48f+(float)Math.sin(animTime*3.1f+e.phase)*h*.025f:floor;
    }
    private boolean overlaps(float l1,float t1,float r1,float b1,float l2,float t2,float r2,float b2) {
        return l1<r2 && r1>l2 && t1<b2 && b1>t2;
    }
    private boolean enemyOverlapsPlayer(Enemy e,float px,float py) {
        float playerL=px+bunnyW*.13f,playerR=px+bunnyW*.87f;
        float playerT=py+bunnyH*.13f,playerB=py+bunnyH*.98f;
        float feet=enemyFeetY(e),half=e.size*(e.flying?.57f:.68f);
        float top=feet-e.size*(e.flying?1.05f:1.22f);
        return overlaps(playerL,playerT,playerR,playerB,e.x-half,top,e.x+half,feet);
    }
    private boolean enemyTouchingPlayer(Enemy e,float px,float py,float dir) {
        float half=e.size*(e.flying?.57f:.68f),feet=enemyFeetY(e),top=feet-e.size*(e.flying?1.05f:1.22f);
        float playerTop=py+bunnyH*.13f,playerBottom=py+bunnyH*.98f;
        if(playerTop>=feet||playerBottom<=top)return false;
        float gap=dir>=0?Math.abs(px+bunnyW*.87f-(e.x-half)):Math.abs(px+bunnyW*.13f-(e.x+half));
        return gap<=unit*2f;
    }
    private void resolvePlayerCollisions(float previousX,float dir) {
        for(Enemy e:enemies) {
            if(e.dead || !enemyOverlapsPlayer(e,playerX,playerY)) continue;
            boolean wasOverlapping=enemyOverlapsPlayer(e,previousX,playerY)||enemyTouchingPlayer(e,previousX,playerY,dir);
            if(!wasOverlapping)hurt();
            if(!wasOverlapping || dir>0&&e.x>=previousX || dir<0&&e.x<=previousX) {
                float half=e.size*(e.flying?.57f:.68f);
                if(dir>0) playerX=Math.min(playerX,e.x-half-bunnyW*.87f);
                else if(dir<0) playerX=Math.max(playerX,e.x+half-bunnyW*.13f);
            }
        }
        for(Hazard z:hazards) {
            if(z.type==1) continue; // Buracos são tratados como queda, nunca como colisão sólida.
            float floor=terrainY(z.x),left,right,top,bottom=floor;
            if(z.type==1) { left=z.x-z.size*.08f; right=z.x+z.size*1.08f; top=floor-z.size*.58f; }
            else { float half=z.size*(z.type==0?.57f:.52f); left=z.x-half;right=z.x+half;top=floor-z.size*(z.type==0?1.08f:.86f); }
            if(!overlaps(playerX+bunnyW*.13f,playerY+bunnyH*.13f,playerX+bunnyW*.87f,playerY+bunnyH*.98f,left,top,right,bottom)) continue;
            boolean wasOverlapping=overlaps(previousX+bunnyW*.13f,playerY+bunnyH*.13f,previousX+bunnyW*.87f,playerY+bunnyH*.98f,left,top,right,bottom);
            float previousEdge=dir>=0?previousX+bunnyW*.87f:previousX+bunnyW*.13f;
            float contactEdge=dir>=0?left:right;
            boolean atContact=dir==0?false:Math.abs(previousEdge-contactEdge)<=unit*2f && previousYOverlapsHazard(previousX,playerY,top,bottom);
            if(!wasOverlapping&&!atContact)hurt();
            wasOverlapping=wasOverlapping||atContact;
            if(!wasOverlapping || dir>0&&z.x>=previousX || dir<0&&z.x<=previousX) {
                if(dir>0) playerX=Math.min(playerX,left-bunnyW*.87f);
                else if(dir<0) playerX=Math.max(playerX,right-bunnyW*.13f);
            }
        }
        if(onGround) { ground=terrainY(playerX); playerY=ground-bunnyH; }
    }
    private boolean previousYOverlapsHazard(float px,float py,float top,float bottom) {
        return py+bunnyH*.13f<bottom && py+bunnyH*.98f>top;
    }

    private void shoot() {
        if(state!=1)return;
        float direction=(leftHeld&&!rightHeld)?-1:(rightHeld&&!leftHeld)?1:(facingRight?1:-1);
        lastShotDirection=direction;
        Log.d("CoelhitosInput","shoot dir="+(int)direction+" moving="+(leftHeld||rightHeld));
        float sx=playerX+(direction>0?bunnyW*.94f:-bunnyW*.04f), sy=playerY+bunnyH*.56f, v=650*unit*direction;
        if(power==THORN) {
            float targetX=playerX+direction*bunnyW*1.8f;
            shots.add(new Shot(targetX, terrainY(targetX)-52*unit, 0, 0, 1, 4, 0));
            audio.effect(10);
        } else if(power==FEATHER) {
            for(int i=-1;i<=1;i++) shots.add(new Shot(sx,sy+i*9*unit,v,-i*80*unit,1,2,0));
            audio.effect(11);
        } else if(power==FIRE) {
            shots.add(new Shot(sx,sy,v,0,2,3,0)); audio.effect(12);
        } else if(carrots>0) {
            carrots--; shots.add(new Shot(sx,sy,v,0,1,1,0)); audio.effect(3);
        } else { say("Encontre cenouras ou um poder"); audio.effect(13); }
    }

    private void drawWorld(Canvas c) {
        if(levelNumber==2) drawCaveBackground(c);
        else {
            p.setShader(new LinearGradient(0,0,0,h,Color.rgb(72,175,239),Color.rgb(235,249,255),Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,p); p.setShader(null);
            drawSun(c);
            drawClouds(c);
            drawHills(c);
        }
        for(Decor d:decor) {
            float sx=d.x-camera*.58f;
            if(sx<-120*unit || sx>w+120*unit)continue;
            drawDecor(c,sx,d);
        }
        drawGround(c);
        for(Hazard z:hazards) drawHazard(c,z);
        for(Pickup q:pickups) drawPickup(c,q);
        for(Enemy e:enemies) if(!e.dead) drawEnemy(c,e);
        drawPortal(c);
        if(state!=0 && state!=2) drawRabbit(c,playerX-camera,playerY);
        for(Shot s:shots) drawShot(c,s);
        for(Shot s:enemyShots) drawShot(c,s);
        for(Burst b:bursts) drawBurst(c,b);
    }

    private void drawCaveBackground(Canvas c) {
        p.setShader(new LinearGradient(0,0,0,h,Color.rgb(31,39,105),Color.rgb(103,71,147),Shader.TileMode.CLAMP));
        c.drawRect(0,0,w,h,p);p.setShader(null);
        float shift=(camera*.11f)%(w*.38f);
        Path back=new Path();back.moveTo(0,h*.58f);
        for(int i=-1;i<5;i++) {
            float x=i*w*.38f-shift;
            back.lineTo(x+w*.04f,h*.34f);back.lineTo(x+w*.15f,h*.44f);
            back.lineTo(x+w*.23f,h*.26f);back.lineTo(x+w*.38f,h*.56f);
        }
        back.lineTo(w,h*.68f);back.lineTo(0,h*.68f);back.close();
        p.setShader(new LinearGradient(0,h*.23f,0,h*.7f,Color.rgb(76,78,166),Color.rgb(53,57,133),Shader.TileMode.CLAMP));c.drawPath(back,p);p.setShader(null);
        p.setColor(Color.argb(80,201,186,255));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2*unit);c.drawPath(back,p);p.setStyle(Paint.Style.FILL);
        for(int i=0;i<8;i++) {
            float x=(i*w*.19f-camera*.24f)%(w+100*unit);if(x< -30*unit)x+=w+100*unit;
            float top=h*(.12f+(i%3)*.07f),len=h*(.12f+(i%4)*.045f);
            Path stal=new Path();stal.moveTo(x-12*unit,top);stal.lineTo(x+12*unit,top);stal.lineTo(x,top+len);stal.close();
            p.setShader(new LinearGradient(x-8*unit,top,x+8*unit,top+len,Color.rgb(112,98,177),Color.rgb(47,50,119),Shader.TileMode.CLAMP));c.drawPath(stal,p);p.setShader(null);
        }
        for(int i=0;i<5;i++) {
            float x=(i*w*.27f-camera*.32f)%(w+120*unit);if(x< -40*unit)x+=w+120*unit;
            float y=h*(.47f+(i%3)*.055f);
            drawCrystal(c,x,y,26*unit,i%2==0?0:1);
        }
        p.setShader(new LinearGradient(0,h*.5f,0,h*.91f,Color.argb(0,105,236,247),Color.argb(42,94,230,242),Shader.TileMode.CLAMP));
        c.drawRect(0,h*.45f,w,h*.92f,p);p.setShader(null);
    }

    private void drawCrystal(Canvas c,float x,float y,float size,int variant) {
        int glow=variant==0?Color.rgb(82,245,244):Color.rgb(198,115,255);
        int facet=variant==0?Color.rgb(116,247,239):Color.rgb(222,165,255);
        p.setColor(Color.argb(28,Color.red(glow),Color.green(glow),Color.blue(glow)));c.drawCircle(x,y-size*.35f,size*.78f,p);
        Path gem=new Path();gem.moveTo(x-size*.36f,y);gem.lineTo(x-size*.22f,y-size*.83f);gem.lineTo(x+size*.16f,y-size);gem.lineTo(x+size*.4f,y-size*.22f);gem.lineTo(x+size*.08f,y+size*.42f);gem.close();
        p.setShader(new LinearGradient(x-size*.25f,y-size*.8f,x+size*.25f,y+size*.25f,facet,glow,Shader.TileMode.CLAMP));c.drawPath(gem,p);p.setShader(null);
        Path shine=new Path();shine.moveTo(x-size*.18f,y-size*.72f);shine.lineTo(x+size*.08f,y-size*.88f);shine.lineTo(x+size*.02f,y-size*.1f);shine.close();
        p.setColor(Color.argb(165,244,255,255));c.drawPath(shine,p);
    }

    private void drawClouds(Canvas c) {
        p.setColor(Color.argb(30,35,76,116));
        for(int i=0;i<6;i++) {
            float x=(i*245*unit-camera*.19f)%(w+230*unit); if(x< -100*unit)x+=w+230*unit;
            float y=h*(.11f+(i%3)*.12f);
            c.drawOval(x,y+5*unit,x+72*unit,y+31*unit,p); c.drawCircle(x+25*unit,y+5*unit,18*unit,p); c.drawCircle(x+48*unit,y+6*unit,15*unit,p);
            p.setColor(Color.rgb(255,255,255));
            c.drawOval(x,y,x+72*unit,y+25*unit,p); c.drawCircle(x+25*unit,y,18*unit,p); c.drawCircle(x+48*unit,y+1*unit,15*unit,p);
            p.setColor(Color.argb(30,35,76,116));
        }
    }
    private void drawSun(Canvas c) {
        float x=w*.82f,y=h*.17f,r=h*.055f;
        p.setColor(Color.argb(35,255,208,91)); c.drawCircle(x,y,r*1.8f,p);
        p.setShader(new LinearGradient(x-r,y-r,x+r,y+r,Color.rgb(255,249,190),Color.rgb(255,190,91),Shader.TileMode.CLAMP));
        c.drawCircle(x,y,r,p);p.setShader(null);
    }
    private void drawHills(Canvas c) {
        float backShift=(camera*.105f)%(w*.42f);
        Path back=new Path();back.moveTo(-w,h*.69f);
        for(int i=-2;i<5;i++){float x=i*w*.42f-backShift;back.quadTo(x+w*.12f,h*(.39f+(i%2)*.035f),x+w*.42f,h*.69f);}
        back.lineTo(w*2,h*.74f);back.lineTo(-w,h*.74f);back.close();
        p.setShader(new LinearGradient(0,h*.37f,0,h*.75f,Color.rgb(192,235,207),Color.rgb(118,194,151),Shader.TileMode.CLAMP));c.drawPath(back,p);p.setShader(null);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.4f*unit);p.setColor(Color.argb(100,245,255,227));c.drawPath(back,p);p.setStyle(Paint.Style.FILL);
        float nearShift=(camera*.20f)%(w*.53f);
        Path near=new Path();near.moveTo(-w,h*.79f);
        for(int i=-2;i<5;i++){float x=i*w*.53f-nearShift;near.quadTo(x+w*.15f,h*(.51f+(i%2)*.045f),x+w*.53f,h*.79f);}
        near.lineTo(w*2,h*.84f);near.lineTo(-w,h*.84f);near.close();
        p.setShader(new LinearGradient(0,h*.50f,0,h*.85f,Color.rgb(152,220,155),Color.rgb(92,180,119),Shader.TileMode.CLAMP));c.drawPath(near,p);p.setShader(null);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3*unit);p.setColor(Color.argb(115,225,250,171));c.drawPath(near,p);p.setStyle(Paint.Style.FILL);
        // Morros distantes com faces retas e picos suaves para quebrar a linha uniforme do horizonte.
        float ridgeShift=(camera*.075f)%(w*.34f);
        Path ridge=new Path();ridge.moveTo(-w,h*.70f);
        for(int i=-2;i<7;i++) {
            float x=i*w*.34f-ridgeShift, base=h*(.68f+(i%2)*.025f), peak=h*(.43f+(i%3)*.035f);
            ridge.lineTo(x+w*.10f,base);ridge.lineTo(x+w*.22f,peak);ridge.lineTo(x+w*.34f,base);
        }
        ridge.lineTo(w*2,h*.76f);ridge.lineTo(-w,h*.76f);ridge.close();
        p.setShader(new LinearGradient(0,h*.43f,0,h*.77f,Color.rgb(176,222,190),Color.rgb(125,190,152),Shader.TileMode.CLAMP));c.drawPath(ridge,p);p.setShader(null);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2*unit);p.setColor(Color.argb(75,250,255,216));c.drawPath(ridge,p);p.setStyle(Paint.Style.FILL);
    }
    private void drawGround(Canvas c) {
        if(levelNumber==2) { drawCaveGround(c); return; }
        float step=34*unit;Path soil=new Path();soil.moveTo(-step,terrainY(camera-step));
        for(float x=-step;x<=w+step;x+=step)soil.lineTo(x,terrainY(Math.max(0,camera+x)));
        soil.lineTo(w+step,h);soil.lineTo(-step,h);soil.close();
        p.setShader(new LinearGradient(0,h*.70f,0,h,Color.rgb(157,218,107),Color.rgb(62,151,86),Shader.TileMode.CLAMP));c.drawPath(soil,p);p.setShader(null);
        Path edge=new Path();edge.moveTo(-step,terrainY(camera-step));for(float x=-step;x<=w+step;x+=step)edge.lineTo(x,terrainY(Math.max(0,camera+x)));
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(7*unit);p.setColor(Color.argb(100,43,122,72));c.drawPath(edge,p);
        p.setStrokeWidth(3.2f*unit);p.setColor(Color.rgb(106,190,86));c.drawPath(edge,p);
        p.setStrokeWidth(1.3f*unit);p.setColor(Color.rgb(225,248,155));c.drawPath(edge,p);p.setStyle(Paint.Style.FILL);
        for(int i=0;i<36;i++) {
            float x=(i*107*unit-camera*.93f)%(w+30*unit);if(x<0)x+=w+30*unit;
            float y=terrainY(camera+x)+20*unit+(i%4)*12*unit;
            p.setColor(Color.argb(34,31,104,59));c.drawOval(x-5*unit,y-2*unit,x+13*unit,y+4*unit,p);
            p.setColor(i%3==0?Color.rgb(93,164,75):Color.rgb(77,151,76));
            for(int k=0;k<3;k++)c.drawLine(x+k*3*unit,y,x+(k-1)*2*unit,y-5*unit,p);
        }
    }
    private void drawCaveGround(Canvas c) {
        float step=24*unit;
        Path floor=new Path();floor.moveTo(-step,terrainY(Math.max(0,camera-step)));
        for(float x=-step;x<=w+step;x+=step)floor.lineTo(x,terrainY(Math.max(0,camera+x)));
        floor.lineTo(w+step,h);floor.lineTo(-step,h);floor.close();
        p.setShader(new LinearGradient(0,h*.63f,0,h,Color.rgb(96,125,184),Color.rgb(39,47,103),Shader.TileMode.CLAMP));c.drawPath(floor,p);p.setShader(null);
        Path rim=new Path();rim.moveTo(-step,terrainY(Math.max(0,camera-step)));
        for(float x=-step;x<=w+step;x+=step)rim.lineTo(x,terrainY(Math.max(0,camera+x)));
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(8*unit);p.setColor(Color.rgb(44,48,105));c.drawPath(rim,p);
        p.setStrokeWidth(4*unit);p.setColor(Color.rgb(100,220,203));c.drawPath(rim,p);
        p.setStrokeWidth(1.6f*unit);p.setColor(Color.argb(190,209,255,224));c.drawPath(rim,p);p.setStyle(Paint.Style.FILL);
        for(int i=0;i<24;i++) {
            float x=(i*131*unit-camera*.91f)%(w+40*unit);if(x<0)x+=w+40*unit;
            float y=terrainY(camera+x)+18*unit+(i%3)*10*unit;
            p.setColor(i%2==0?Color.rgb(116,227,207):Color.rgb(172,121,237));
            c.drawCircle(x,y,1.4f*unit,p);
        }
    }
    private float terrainY(float worldX) {
        if(levelNumber==2) {
            float y=h*.80f;
            y-=hill(worldX,levelLength*.18f,levelLength*.12f,h*.095f);
            y-=hill(worldX,levelLength*.49f,levelLength*.11f,h*.12f);
            y-=hill(worldX,levelLength*.80f,levelLength*.13f,h*.10f);
            y-=ramp(worldX,levelLength*.105f,levelLength*.116f,h*.16f);
            y+=ramp(worldX,levelLength*.130f,levelLength*.141f,h*.16f);
            y+=ramp(worldX,levelLength*.30f,levelLength*.312f,h*.17f);
            y-=ramp(worldX,levelLength*.329f,levelLength*.341f,h*.17f);
            y-=ramp(worldX,levelLength*.53f,levelLength*.541f,h*.19f);
            y+=ramp(worldX,levelLength*.563f,levelLength*.575f,h*.19f);
            y+=ramp(worldX,levelLength*.72f,levelLength*.732f,h*.15f);
            y-=ramp(worldX,levelLength*.75f,levelLength*.762f,h*.15f);
            y-=ramp(worldX,levelLength*.86f,levelLength*.872f,h*.18f);
            y+=ramp(worldX,levelLength*.892f,levelLength*.904f,h*.18f);
            return y;
        }
        float y=h*.82f;
        y-=hill(worldX,levelLength*.17f,levelLength*.105f,h*.105f);
        y-=hill(worldX,levelLength*.42f,levelLength*.13f,h*.075f);
        y-=hill(worldX,levelLength*.68f,levelLength*.105f,h*.13f);
        y-=hill(worldX,levelLength*.87f,levelLength*.095f,h*.085f);
        y-=ramp(worldX,levelLength*.485f,levelLength*.497f,h*.205f);
        y+=ramp(worldX,levelLength*.514f,levelLength*.526f,h*.205f);
        y-=ramp(worldX,levelLength*.735f,levelLength*.744f,h*.17f);
        y+=ramp(worldX,levelLength*.758f,levelLength*.769f,h*.17f);
        y-=ramp(worldX,levelLength*.095f,levelLength*.101f,h*.14f);
        y+=ramp(worldX,levelLength*.106f,levelLength*.112f,h*.14f);
        y-=ramp(worldX,levelLength*.285f,levelLength*.292f,h*.16f);
        y+=ramp(worldX,levelLength*.300f,levelLength*.307f,h*.16f);
        y-=ramp(worldX,levelLength*.605f,levelLength*.611f,h*.15f);
        y+=ramp(worldX,levelLength*.620f,levelLength*.626f,h*.15f);
        y-=ramp(worldX,levelLength*.895f,levelLength*.901f,h*.13f);
        y+=ramp(worldX,levelLength*.910f,levelLength*.916f,h*.13f);
        return y;
    }
    private float ramp(float x,float start,float end,float rise) {
        if(x<=start)return 0;if(x>=end)return rise;
        return rise*(x-start)/(end-start);
    }
    private float hill(float x,float center,float halfWidth,float rise) {
        float t=Math.abs(x-center)/halfWidth;if(t>=1)return 0;
        return rise*(.5f+.5f*(float)Math.cos(Math.PI*t));
    }
    private void drawDecor(Canvas c,float x,Decor d) {
        float y=terrainY(d.x), s=d.scale*unit;
        if(d.kind==6) {
            p.setColor(Color.argb(55,18,27,68));c.drawOval(x-23*s,y-3*s,x+23*s,y+5*s,p);
            drawCrystal(c,x,y+2*s,42*s,(int)(d.x/unit)%2==0?0:1);
        } else if(d.kind==0) {
            p.setColor(Color.argb(40,42,86,54));c.drawOval(x-27*s,y-4*s,x+28*s,y+6*s,p);
            p.setShader(new LinearGradient(x-8*s,y-74*s,x+12*s,y,Color.rgb(191,139,83),Color.rgb(113,75,52),Shader.TileMode.CLAMP));
            c.drawRoundRect(x-8*s,y-75*s,x+8*s,y,7*s,7*s,p);p.setShader(null);
            p.setShader(new LinearGradient(x-23*s,y-122*s,x+28*s,y-54*s,Color.rgb(163,230,120),Color.rgb(36,142,82),Shader.TileMode.CLAMP));c.drawCircle(x,y-83*s,31*s,p);p.setShader(null);
            p.setShader(new LinearGradient(x-28*s,y-115*s,x-7*s,y-75*s,Color.rgb(189,244,143),Color.rgb(95,194,99),Shader.TileMode.CLAMP));c.drawCircle(x-14*s,y-94*s,20*s,p);p.setShader(null);
            p.setShader(new LinearGradient(x+3*s,y-113*s,x+28*s,y-75*s,Color.rgb(132,221,112),Color.rgb(48,160,88),Shader.TileMode.CLAMP));c.drawCircle(x+15*s,y-96*s,19*s,p);p.setShader(null);
            p.setColor(Color.argb(105,238,255,191));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.2f*s);c.drawArc(x-27*s,y-113*s,x+4*s,y-61*s,190,105,false,p);p.setStyle(Paint.Style.FILL);
        } else if(d.kind==1) {
            p.setColor(Color.argb(38,41,82,48));c.drawOval(x-29*s,y-3*s,x+35*s,y+5*s,p);
            p.setShader(new LinearGradient(x,y-37*s,x,y+3*s,Color.rgb(119,213,106),Color.rgb(36,137,78),Shader.TileMode.CLAMP));
            c.drawCircle(x-9*s,y-17*s,22*s,p);c.drawCircle(x+14*s,y-14*s,20*s,p);c.drawCircle(x+1*s,y-27*s,20*s,p);p.setShader(null);
            p.setColor(Color.argb(90,233,255,176));c.drawCircle(x-5*s,y-31*s,4*s,p);
        } else if(d.kind==2 || d.kind==4) {
            p.setColor(Color.rgb(63,146,79));p.setStrokeWidth(2*s);c.drawLine(x,y,x+2*s,y-30*s,p);
            int petal=d.kind==2?Color.rgb(255,216,102):Color.rgb(255,161,183);
            p.setShader(new LinearGradient(x-9*s,y-40*s,x+8*s,y-22*s,Color.WHITE,petal,Shader.TileMode.CLAMP));
            for(int a=0;a<8;a++){double ang=a*Math.PI/4;c.drawCircle(x+(float)Math.cos(ang)*10*s,y-31*s+(float)Math.sin(ang)*10*s,5.5f*s,p);}p.setShader(null);
            p.setColor(Color.rgb(255,218,91));c.drawCircle(x,y-31*s,5*s,p);
            if(d.kind==4){p.setColor(Color.rgb(255,255,239));c.drawCircle(x+24*s,y-13*s,4*s,p);p.setColor(Color.rgb(233,134,174));c.drawCircle(x+24*s,y-13*s,2*s,p);}
        } else if(d.kind==3) {
            p.setColor(Color.argb(42,38,70,59));c.drawOval(x-20*s,y-2*s,x+22*s,y+6*s,p);
            p.setShader(new LinearGradient(x-8*s,y-16*s,x+10*s,y+4*s,Color.rgb(222,210,178),Color.rgb(133,142,121),Shader.TileMode.CLAMP));
            c.drawOval(x-18*s,y-17*s,x+18*s,y+3*s,p);p.setShader(null);
            p.setColor(Color.argb(145,255,255,224));c.drawArc(x-14*s,y-14*s,x+7*s,y-1*s,190,90,false,p);
        } else {
            p.setColor(Color.rgb(69,153,85));p.setStrokeWidth(2*s);c.drawLine(x-5*s,y,x-6*s,y-21*s,p);c.drawLine(x+6*s,y,x+8*s,y-17*s,p);
            p.setColor(Color.rgb(255,248,217));c.drawCircle(x-6*s,y-24*s,7*s,p);p.setColor(Color.rgb(244,131,151));c.drawCircle(x-6*s,y-24*s,4*s,p);
            p.setColor(Color.rgb(255,220,103));c.drawCircle(x+8*s,y-20*s,6*s,p);p.setColor(Color.rgb(255,255,240));c.drawCircle(x+8*s,y-20*s,2.5f*s,p);
        }
    }
    private void drawHazard(Canvas c,Hazard z) {
        float x=z.x-camera,s=z.size;
        if(x<-s*2||x>w+s*2)return;
        float g=terrainY(z.x);
        p.setShader(null); p.setStyle(Paint.Style.FILL); p.setAlpha(255);
        p.setColor(Color.argb(55,34,48,42));
        c.drawOval(x-s*.72f,g-3*unit,x+s*.72f,g+7*unit,p);
        if(z.type==1 && levelNumber==2) {
            float half=s*.47f;
            p.setColor(Color.argb(95,10,14,42));c.drawOval(x-half-4*unit,g-5*unit,x+half+4*unit,g+13*unit,p);
            p.setShader(new LinearGradient(x,g,x,g+s*.72f,Color.rgb(11,15,48),Color.rgb(3,5,20),Shader.TileMode.CLAMP));
            c.drawRect(x-half,g-3*unit,x+half,g+s*.72f,p);p.setShader(null);
            p.setColor(Color.rgb(36,38,83));c.drawOval(x-half,g-s*.11f,x+half,g+s*.12f,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3*unit);p.setColor(Color.rgb(139,119,194));
            c.drawArc(x-half,g-s*.12f,x+half,g+s*.13f,180,180,false,p);
            p.setStrokeWidth(1.5f*unit);p.setColor(Color.rgb(87,239,229));c.drawArc(x-half,g-s*.10f,x+half,g+s*.10f,185,170,false,p);p.setStyle(Paint.Style.FILL);
        } else if(z.type==1) {
            drawSpriteAtBottom(c,pitSprite,x+s*.5f,g+s*.17f,s*1.12f,s*.70f);
        } else if(z.type==0) {
            drawSpriteAtBottom(c,spikeSprite,x,g+s*.06f,s*1.72f,s*1.34f);
        } else {
            drawSpriteAtBottom(c,fireSprite,x,g+s*.05f,s*1.58f,s*1.34f);
        }
    }
    private float drawSpriteAtBottom(Canvas c,Bitmap sprite,float centerX,float bottomY,float maxW,float maxH) {
        if(sprite==null)return 0;
        float aspect=sprite.getWidth()/(float)sprite.getHeight();
        float height=Math.min(maxH,maxW/aspect),width=height*aspect;
        p.setShader(null);p.setStyle(Paint.Style.FILL);p.setAlpha(255);p.setColor(Color.WHITE);
        c.drawBitmap(sprite,null,new RectF(centerX-width*.5f,bottomY-height,centerX+width*.5f,bottomY),p);
        return height;
    }
    private void drawPickup(Canvas c,Pickup q) {
        float pickupGround=terrainY(q.x);
        float x=q.x-camera, y=q.type==2?pickupGround-bunnyH*.65f:pickupGround-bunnyH*.25f;
        if(x<-35*unit||x>w+35*unit)return;
        if(q.type==0) {
            p.setColor(Color.rgb(243,142,37)); RectF r=new RectF(x-13*unit,y-6*unit,x+13*unit,y+7*unit); c.save(); c.rotate(-24,x,y); c.drawRoundRect(r,7*unit,7*unit,p); c.restore();
            p.setColor(Color.rgb(63,169,76)); Path leaf=new Path(); leaf.moveTo(x+7*unit,y-4*unit); leaf.quadTo(x+6*unit,y-16*unit,x+18*unit,y-14*unit); leaf.quadTo(x+15*unit,y-6*unit,x+7*unit,y-4*unit); c.drawPath(leaf,p);
        } else if(q.type==1) {
            p.setColor(Color.rgb(241,74,100)); c.drawCircle(x-5*unit,y-2*unit,7*unit,p); c.drawCircle(x+5*unit,y-2*unit,7*unit,p); Path heart=new Path();heart.moveTo(x-12*unit,y);heart.lineTo(x,y+12*unit);heart.lineTo(x+12*unit,y);heart.close();c.drawPath(heart,p);
        } else drawChest(c,x,y);
    }
    private void drawChest(Canvas c,float x,float y) {
        float s=unit;
        p.setColor(Color.rgb(142,83,44)); c.drawRoundRect(x-18*s,y-16*s,x+18*s,y+12*s,5*s,5*s,p);
        p.setColor(Color.rgb(226,161,66)); c.drawRoundRect(x-19*s,y-22*s,x+19*s,y-8*s,6*s,6*s,p);
        p.setColor(Color.rgb(255,220,105)); c.drawRect(x-3*s,y-16*s,x+3*s,y+12*s,p);
        p.setColor(Color.rgb(255,245,178)); c.drawCircle(x,y-14*s,3*s,p);
    }
    private void drawEnemy(Canvas c,Enemy e) {
        float floor=terrainY(e.x),baseY=enemyFeetY(e),x=e.x-camera;
        if(x<-e.size*2||x>w+e.size*2)return;
        p.setShader(null);p.setStyle(Paint.Style.FILL);p.setAlpha(255);
        float shadowScale=e.flying?.72f:.94f;
        p.setShader(new LinearGradient(x,floor-3*unit,x,floor+8*unit,Color.argb(72,33,54,43),Color.argb(0,33,54,43),Shader.TileMode.CLAMP));
        c.drawOval(x-e.size*shadowScale,floor-4*unit,x+e.size*shadowScale,floor+7*unit,p);p.setShader(null);
        float wag=(float)Math.sin(animTime*8.7f+e.phase);
        if(e.tail) drawEnemyTail(c,x,baseY,e.size,wag);
        if(e.type==Enemy.FLYING) drawScarf(c,x,baseY,e.size,wag);
        float gait=(float)Math.sin(animTime*8f+e.phase),spriteH;
        float bodyBob=Math.max(0,gait)*e.size*.045f,bodyY=baseY-bodyBob;
        if(e.type==Enemy.PLANT_SHOOTER) { spriteH=drawPlantShooter(c,e,x,bodyY,gait);drawEnemyWalk(c,e,x,baseY,gait); }
        else if(e.type==Enemy.BEETLE) spriteH=drawBeetle(c,e,x,baseY,gait);
        else if(e.type==Enemy.MOTH) spriteH=drawMoth(c,e,x,baseY,gait);
        else if(e.type==Enemy.FLYING) spriteH=drawFlyingEnemy(c,e,x,baseY,gait);
        else if(e.type==Enemy.HORNED) spriteH=drawHornedEnemy(c,e,x,bodyY,gait);
        else if(e.type==Enemy.TAIL) spriteH=drawTailCreature(c,e,x,bodyY,gait);
        else if(e.big) spriteH=drawShellCreature(c,e,x,bodyY,gait);
        else {
            Bitmap sprite=e.big?enemyLargeSprite:enemySmallSprite;
            float bodyFloor=bodyY;
            c.save();c.translate(x,bodyFloor);c.scale(e.tail?1.22f:1f, e.tail?.84f:1f);c.scale(1f+Math.abs(gait)*.025f,1f-Math.abs(gait)*.035f);c.translate(-x,-bodyFloor);
            spriteH=drawSpriteAtBottom(c,sprite,x,bodyFloor,e.size*1.86f,e.size*1.34f);c.restore();
            if(e.tail) drawEnemyRings(c,x,baseY,e.size,wag);
        }
        if(!e.flying && e.type!=Enemy.PLANT_SHOOTER && e.type!=Enemy.BEETLE) drawEnemyWalk(c,e,x,baseY,gait);
        float blink=(animTime+e.phase*.35f)%3.6f;
        if(blink<.13f) {
            float eyeY=baseY-spriteH*.40f;
            p.setColor(e.type==Enemy.HORNED?Color.rgb(244,191,78):(e.flying?Color.rgb(99,205,177):(e.big?Color.rgb(189,165,223):Color.rgb(148,208,239))));p.setStrokeWidth(2.2f*unit);p.setStrokeCap(Paint.Cap.ROUND);
            c.drawLine(x+e.size*.20f,eyeY,x+e.size*.31f,eyeY+1.5f*unit,p);
            c.drawLine(x+e.size*.48f,eyeY+2*unit,x+e.size*.58f,eyeY,p);
            p.setStrokeCap(Paint.Cap.BUTT);
        }
        if(e.maxHp>1) {
            float top=baseY-spriteH-8*unit;
            p.setColor(Color.argb(220,255,255,255)); c.drawRoundRect(x-e.size*.42f,top,x+e.size*.42f,top+7*unit,4*unit,4*unit,p);
            p.setColor(Color.rgb(237,92,103)); c.drawRoundRect(x-e.size*.39f,top+1.2f*unit,x-e.size*.39f+e.size*.78f*Math.max(0,e.hp)/(float)e.maxHp,top+5.8f*unit,3*unit,3*unit,p);
        }
    }
    private float drawFlyingEnemy(Canvas c,Enemy e,float x,float feet,float flap) {
        float s=e.size, mid=feet-s*.53f, wingY=mid-s*.02f;
        Path wing=new Path();wing.moveTo(x-s*.28f,wingY);wing.quadTo(x-s*.72f,wingY-s*(.25f+Math.abs(flap)*.2f),x-s*.46f,wingY-s*(.55f+Math.max(0,flap)*.18f));wing.quadTo(x-s*.05f,wingY-s*.38f,x+s*.04f,wingY);wing.close();
        p.setShader(new LinearGradient(x-s*.5f,wingY-s*.5f,x,wingY,Color.rgb(112,233,195),Color.rgb(30,153,143),Shader.TileMode.CLAMP));c.drawPath(wing,p);p.setShader(null);
        p.setShader(new LinearGradient(x-s*.45f,mid-s*.32f,x+s*.48f,feet,Color.rgb(162,244,215),Color.rgb(39,167,150),Shader.TileMode.CLAMP));
        c.drawOval(x-s*.52f,mid-s*.37f,x+s*.43f,feet-s*.08f,p);p.setShader(null);
        c.drawCircle(x+s*.27f,mid-s*.24f,s*.28f,p);
        Path beak=new Path();beak.moveTo(x+s*.43f,mid-s*.19f);beak.lineTo(x+s*.77f,mid-s*.10f);beak.lineTo(x+s*.43f,mid+s*.02f);beak.close();
        p.setShader(new LinearGradient(x+s*.43f,mid-s*.2f,x+s*.72f,mid,Color.rgb(255,224,105),Color.rgb(235,139,42),Shader.TileMode.CLAMP));c.drawPath(beak,p);p.setShader(null);
        p.setColor(Color.WHITE);c.drawCircle(x+s*.30f,mid-s*.28f,s*.10f,p);p.setColor(Color.rgb(43,59,73));c.drawCircle(x+s*.34f,mid-s*.27f,s*.055f,p);
        p.setColor(Color.rgb(237,164,71));p.setStrokeWidth(s*.055f);p.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x-s*.13f,feet-s*.09f,x-s*.18f,feet,p);c.drawLine(x+s*.05f,feet-s*.09f,x+s*.03f,feet,p);p.setStrokeCap(Paint.Cap.BUTT);
        return s*.94f;
    }
    private float drawPlantShooter(Canvas c,Enemy e,float x,float feet,float gait) {
        float s=e.size, bob=Math.max(0,gait)*s*.025f, y=feet-bob;
        p.setColor(Color.argb(65,27,37,26));c.drawOval(x-s*.42f,feet-2*unit,x+s*.46f,feet+4*unit,p);
        p.setShader(new LinearGradient(x-s*.36f,y-s*.72f,x+s*.3f,y,Color.rgb(255,239,196),Color.rgb(208,166,108),Shader.TileMode.CLAMP));
        c.drawOval(x-s*.40f,y-s*.76f,x+s*.42f,y-s*.03f,p);p.setShader(null);
        p.setShader(new LinearGradient(x-s*.32f,y-s*.54f,x+s*.06f,y-s*.22f,Color.rgb(179,242,118),Color.rgb(45,165,91),Shader.TileMode.CLAMP));
        c.drawOval(x-s*.40f,y-s*1.03f,x-s*.08f,y-s*.56f,p);c.drawOval(x-s*.18f,y-s*1.14f,x+s*.12f,y-s*.60f,p);c.drawOval(x+s*.04f,y-s*.96f,x+s*.37f,y-s*.52f,p);p.setShader(null);
        for(int side=-1;side<=1;side++) {
            float leafX=x+side*s*.27f,leafY=y-s*.32f;
            Path leaf=new Path();leaf.moveTo(x,leafY);leaf.quadTo(leafX,leafY-s*.30f,leafX+side*s*.12f,leafY-s*.38f);leaf.quadTo(leafX-side*s*.05f,leafY-s*.10f,x,leafY);leaf.close();
            p.setShader(new LinearGradient(x,leafY-s*.32f,leafX,leafY,Color.rgb(182,239,115),Color.rgb(46,146,81),Shader.TileMode.CLAMP));c.drawPath(leaf,p);p.setShader(null);
        }
        float muzzleX=x+s*.31f, muzzleY=y-s*.47f;
        p.setShader(new LinearGradient(muzzleX-s*.03f,muzzleY-s*.13f,muzzleX+s*.36f,muzzleY+s*.12f,Color.rgb(105,202,116),Color.rgb(36,125,87),Shader.TileMode.CLAMP));
        c.drawRoundRect(muzzleX-s*.08f,muzzleY-s*.14f,muzzleX+s*.45f,muzzleY+s*.12f,s*.12f,s*.12f,p);p.setShader(null);
        p.setColor(Color.rgb(25,79,69));c.drawCircle(muzzleX+s*.30f,muzzleY-s*.015f,s*.045f,p);
        p.setColor(Color.WHITE);c.drawCircle(x+s*.12f,y-s*.59f,s*.105f,p);p.setColor(Color.rgb(50,57,42));c.drawCircle(x+s*.15f,y-s*.58f,s*.052f,p);
        if(e.attackTime<.45f) {
            p.setColor(Color.rgb(255,207,101));c.drawCircle(muzzleX+s*.47f,muzzleY,s*.11f,p);
        }
        return s*1.12f;
    }
    private float drawBeetle(Canvas c,Enemy e,float x,float feet,float gait) {
        float s=e.size, phase=animTime*9f+e.phase, bob=Math.max(0,(float)Math.sin(phase))*s*.025f, y=feet-bob;
        p.setColor(Color.argb(65,27,31,47));c.drawOval(x-s*.56f,feet-2*unit,x+s*.55f,feet+5*unit,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(s*.075f);
        int[] legColors={Color.rgb(126,95,68),Color.rgb(150,112,73)};
        for(int i=0;i<3;i++)for(int side=-1;side<=1;side+=2) {
            float alternation=(float)Math.sin(phase+((i+(side>0?1:0))%2)*Math.PI);
            float hipX=x+(i-1)*s*.23f,hipY=y-s*(.27f-i*.025f);
            float footX=hipX+side*s*.20f+alternation*s*.09f;
            float footY=feet-s*.018f-Math.max(0,alternation)*s*.045f;
            p.setColor(legColors[i%2]);c.drawLine(hipX,hipY,footX,footY,p);
            p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(218,176,117));c.drawOval(footX-s*.07f,footY-s*.035f,footX+s*.09f,footY+s*.035f,p);p.setStyle(Paint.Style.STROKE);
        }
        p.setStyle(Paint.Style.FILL);p.setStrokeCap(Paint.Cap.BUTT);
        p.setShader(new LinearGradient(x-s*.53f,y-s*.58f,x+s*.50f,y-s*.02f,Color.rgb(230,162,70),Color.rgb(145,72,51),Shader.TileMode.CLAMP));
        c.drawOval(x-s*.55f,y-s*.59f,x+s*.48f,y-s*.04f,p);p.setShader(null);
        p.setShader(new LinearGradient(x-s*.45f,y-s*.67f,x+s*.28f,y-s*.13f,Color.rgb(196,192,173),Color.rgb(82,82,102),Shader.TileMode.CLAMP));
        c.drawOval(x-s*.43f,y-s*.73f,x+s*.35f,y-s*.12f,p);p.setShader(null);
        for(int i=0;i<3;i++) {
            float plateX=x-s*.26f+i*s*.21f, plateY=y-s*(.48f-(i%2)*.04f);
            p.setColor(i%2==0?Color.rgb(255,186,76):Color.rgb(224,129,62));
            c.drawRoundRect(plateX-s*.10f,plateY-s*.10f,plateX+s*.10f,plateY+s*.09f,s*.06f,s*.06f,p);
        }
        p.setColor(Color.WHITE);c.drawCircle(x+s*.30f,y-s*.40f,s*.12f,p);p.setColor(Color.rgb(45,39,43));c.drawCircle(x+s*.34f,y-s*.39f,s*.06f,p);
        return s*.78f;
    }
    private float drawMoth(Canvas c,Enemy e,float x,float feet,float phase) {
        float s=e.size, mid=feet-s*.52f;
        float flap=(float)Math.sin(animTime*12f+e.phase);
        for(int side=-1;side<=1;side+=2) {
            for(int pair=0;pair<2;pair++) {
                float span=s*(pair==0?.64f:.48f), height=s*(pair==0?.52f:.35f)*(0.45f+Math.abs(flap)*.7f);
                float baseX=x+side*s*.08f,baseY=mid-s*.12f+pair*s*.12f;
                Path wing=new Path();wing.moveTo(baseX,baseY);
                wing.quadTo(baseX+side*span*.45f,baseY-side*height*.95f,baseX+side*span,baseY-side*height*.55f);
                wing.quadTo(baseX+side*span*.62f,baseY+side*height*.32f,baseX,baseY+side*s*.06f);wing.close();
                p.setShader(new LinearGradient(baseX,baseY-side*height,baseX+side*span,baseY,Color.rgb(222,192,255),Color.rgb(123,90,190),Shader.TileMode.CLAMP));c.drawPath(wing,p);p.setShader(null);
                p.setColor(Color.argb(220,102,238,222));c.drawCircle(baseX+side*span*.70f,baseY-side*height*.35f,s*.065f,p);
            }
        }
        p.setShader(new LinearGradient(x-s*.32f,mid-s*.32f,x+s*.34f,feet,Color.rgb(233,218,255),Color.rgb(149,104,202),Shader.TileMode.CLAMP));
        c.drawOval(x-s*.38f,mid-s*.32f,x+s*.39f,feet-s*.04f,p);p.setShader(null);
        c.drawCircle(x+s*.24f,mid-s*.25f,s*.25f,p);
        p.setColor(Color.WHITE);c.drawCircle(x+s*.32f,mid-s*.29f,s*.09f,p);p.setColor(Color.rgb(48,40,63));c.drawCircle(x+s*.35f,mid-s*.28f,s*.045f,p);
        p.setColor(Color.rgb(212,173,252));p.setStrokeWidth(s*.045f);p.setStrokeCap(Paint.Cap.ROUND);
        for(int side=-1;side<=1;side+=2)c.drawLine(x+s*.18f,mid-s*.40f,x+s*(.25f+side*.12f),mid-s*.60f,p);
        p.setStrokeCap(Paint.Cap.BUTT);
        return s*.92f;
    }
    private void drawEnemyWalk(Canvas c,Enemy e,float x,float feet,float gait) {
        float s=e.size, phase=animTime*9.5f+e.phase;
        int limb=e.type==Enemy.PLANT_SHOOTER?Color.rgb(73,128,65):(e.type==Enemy.HORNED?Color.rgb(174,98,43):(e.tail?Color.rgb(111,94,153):(e.big?Color.rgb(78,136,86):Color.rgb(44,130,192))));
        int paw=e.type==Enemy.PLANT_SHOOTER?Color.rgb(174,220,123):(e.type==Enemy.HORNED?Color.rgb(255,215,123):(e.tail?Color.rgb(203,181,233):(e.big?Color.rgb(173,211,132):Color.rgb(143,210,245))));
        p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(s*.105f);
        for(int side=-1;side<=1;side+=2) {
            float stride=(float)Math.sin(phase+(side<0?0:Math.PI))*.10f*s;
            float lift=Math.max(0,(float)Math.sin(phase+(side<0?0:Math.PI)))*.075f*s;
            float hipX=x+side*s*.24f,hipY=feet-s*.19f,pawX=hipX+stride,pawY=feet-s*.035f-lift;
            p.setColor(limb);c.drawLine(hipX,hipY,pawX,pawY,p);
            p.setStyle(Paint.Style.FILL);p.setColor(paw);c.drawOval(pawX-s*.12f,pawY-s*.05f,pawX+s*.15f,pawY+s*.045f,p);
            p.setStyle(Paint.Style.STROKE);
        }
        p.setStyle(Paint.Style.FILL);p.setStrokeCap(Paint.Cap.BUTT);
    }
    private float drawTailCreature(Canvas c,Enemy e,float x,float feet,float gait) {
        float s=e.size,body=feet-s*.08f;
        p.setShader(new LinearGradient(x-s*.58f,feet-s*.75f,x+s*.55f,feet,Color.rgb(220,202,246),Color.rgb(128,96,184),Shader.TileMode.CLAMP));
        c.drawOval(x-s*.72f,feet-s*.73f,x+s*.50f,body,p);c.drawCircle(x+s*.36f,feet-s*.67f,s*.31f,p);p.setShader(null);
        p.setColor(Color.argb(95,255,255,255));p.setStrokeWidth(s*.045f);p.setStyle(Paint.Style.STROKE);
        for(int i=0;i<3;i++){float xx=x-s*.38f+i*s*.21f;c.drawArc(xx-s*.09f,feet-s*.58f,xx+s*.09f,feet-s*.23f,195,140,false,p);}p.setStyle(Paint.Style.FILL);
        p.setColor(Color.WHITE);c.drawCircle(x+s*.47f,feet-s*.72f,s*.11f,p);p.setColor(Color.rgb(46,44,67));c.drawCircle(x+s*.51f,feet-s*.71f,s*.055f,p);
        p.setColor(Color.rgb(255,188,196));c.drawCircle(x+s*.66f,feet-s*.49f,s*.075f,p);
        return s*.94f;
    }
    private float drawShellCreature(Canvas c,Enemy e,float x,float feet,float gait) {
        float s=e.size,body=feet-s*.07f;
        p.setShader(new LinearGradient(x-s*.55f,feet-s*.65f,x+s*.55f,feet,Color.rgb(192,238,149),Color.rgb(82,160,91),Shader.TileMode.CLAMP));
        c.drawOval(x-s*.59f,feet-s*.72f,x+s*.58f,body,p);c.drawCircle(x+s*.36f,feet-s*.68f,s*.27f,p);p.setShader(null);
        p.setShader(new LinearGradient(x-s*.38f,feet-s*1.02f,x+s*.18f,feet-s*.36f,Color.rgb(255,224,131),Color.rgb(205,128,63),Shader.TileMode.CLAMP));
        c.drawOval(x-s*.40f,feet-s*1.02f,x+s*.23f,feet-s*.40f,p);p.setShader(null);
        p.setColor(Color.argb(145,255,247,201));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(s*.035f);c.drawArc(x-s*.32f,feet-s*.94f,x+s*.15f,feet-s*.48f,200,120,false,p);p.setStyle(Paint.Style.FILL);
        p.setColor(Color.WHITE);c.drawCircle(x+s*.45f,feet-s*.73f,s*.105f,p);p.setColor(Color.rgb(59,66,42));c.drawCircle(x+s*.49f,feet-s*.72f,s*.052f,p);
        p.setColor(Color.rgb(70,135,75));p.setStrokeWidth(s*.035f);p.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x+s*.20f,feet-s*.85f,x+s*.25f,feet-s*1.08f,p);c.drawLine(x+s*.38f,feet-s*.87f,x+s*.49f,feet-s*1.04f,p);p.setStrokeCap(Paint.Cap.BUTT);
        return s*.98f;
    }
    private float drawHornedEnemy(Canvas c,Enemy e,float x,float feet,float gait) {
        float s=e.size, top=feet-s*1.04f;
        Path plates=new Path();plates.moveTo(x-s*.48f,feet-s*.62f);
        for(int i=0;i<4;i++){float px=x-s*.38f+i*s*.24f;plates.lineTo(px,top+s*(i%2==0?.06f:.16f));plates.lineTo(px+s*.19f,feet-s*.60f);}
        plates.close();p.setShader(new LinearGradient(x-s*.35f,top,x+s*.35f,feet-s*.5f,Color.rgb(250,225,136),Color.rgb(214,140,51),Shader.TileMode.CLAMP));c.drawPath(plates,p);p.setShader(null);
        p.setShader(new LinearGradient(x-s*.55f,top+s*.16f,x+s*.48f,feet,Color.rgb(255,218,122),Color.rgb(207,119,46),Shader.TileMode.CLAMP));
        c.drawOval(x-s*.56f,feet-s*.89f,x+s*.58f,feet-s*.04f,p);c.drawCircle(x+s*.28f,feet-s*.76f,s*.27f,p);p.setShader(null);
        p.setColor(Color.rgb(255,243,203));c.drawCircle(x+s*.35f,feet-s*.78f,s*.105f,p);p.setColor(Color.rgb(69,48,39));c.drawCircle(x+s*.39f,feet-s*.78f,s*.055f,p);
        p.setColor(Color.rgb(247,241,208));c.drawOval(x-s*.44f,feet-s*.13f,x-s*.08f,feet+s*.025f,p);c.drawOval(x+s*.12f,feet-s*.13f,x+s*.48f,feet+s*.025f,p);
        p.setColor(Color.rgb(229,169,71));c.drawCircle(x+s*.5f,feet-s*.43f,s*.075f,p);
        return s*.98f;
    }
    private void drawEnemyRings(Canvas c,float x,float feet,float size,float wag) {
        p.setColor(Color.argb(190,235,207,245));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(size*.045f);
        for(int i=0;i<3;i++){float xx=x-size*(.22f-i*.22f);c.drawArc(xx-size*.08f,feet-size*.68f,xx+size*.08f,feet-size*.4f,185,150,false,p);}p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(240,218,252));c.drawCircle(x-size*(.74f+wag*.1f),feet-size*.13f,size*.07f,p);
    }
    private void drawEnemyTail(Canvas c,float x,float feet,float size,float wag) {
        Path tail=new Path();tail.moveTo(x-size*.58f,feet-size*.27f);
        tail.cubicTo(x-size*(.94f+wag*.08f),feet-size*(.38f+wag*.12f),x-size*(1.02f+wag*.08f),feet-size*(.05f+wag*.16f),x-size*(.72f+wag*.1f),feet-size*.12f);
        p.setColor(Color.argb(100,61,53,80));p.setStrokeWidth(size*.19f);p.setStrokeCap(Paint.Cap.ROUND);p.setStyle(Paint.Style.STROKE);c.drawPath(tail,p);
        p.setColor(Color.rgb(209,190,237));p.setStrokeWidth(size*.13f);c.drawPath(tail,p);p.setStyle(Paint.Style.FILL);p.setStrokeCap(Paint.Cap.BUTT);
        p.setShader(new LinearGradient(x-size*.86f,feet-size*.32f,x-size*.55f,feet-size*.04f,Color.WHITE,Color.rgb(188,159,221),Shader.TileMode.CLAMP));
        c.drawCircle(x-size*(.74f+wag*.1f),feet-size*.13f,size*.15f,p);p.setShader(null);
    }
    private void drawScarf(Canvas c,float x,float feet,float size,float wag) {
        float neckX=x-size*.12f,neckY=feet-size*.79f;
        p.setShader(new LinearGradient(neckX-size*.14f,neckY-size*.08f,neckX+size*.18f,neckY+size*.17f,Color.rgb(255,184,116),Color.rgb(232,91,82),Shader.TileMode.CLAMP));
        c.drawRoundRect(neckX-size*.2f,neckY-size*.07f,neckX+size*.17f,neckY+size*.12f,size*.08f,size*.08f,p);p.setShader(null);
        Path ribbon=new Path();ribbon.moveTo(neckX-size*.12f,neckY);
        ribbon.cubicTo(x-size*(.66f+wag*.13f),neckY-size*(.15f+wag*.18f),x-size*(.83f+wag*.1f),neckY+size*(.12f+wag*.14f),x-size*(1.15f+wag*.12f),neckY+size*(.06f+wag*.16f));
        ribbon.lineTo(x-size*(.88f+wag*.1f),neckY+size*.25f);ribbon.cubicTo(x-size*.55f,neckY+size*.26f,x-size*.34f,neckY+size*.08f,neckX-size*.02f,neckY+size*.1f);ribbon.close();
        p.setShader(new LinearGradient(x-size,neckY-size*.2f,x-size*.4f,neckY+size*.22f,Color.rgb(255,176,122),Color.rgb(205,65,74),Shader.TileMode.CLAMP));c.drawPath(ribbon,p);p.setShader(null);
        p.setColor(Color.rgb(255,202,154));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f*unit);c.drawLine(neckX-size*.18f,neckY-size*.015f,neckX+size*.12f,neckY+size*.04f,p);p.setStyle(Paint.Style.FILL);
    }
    private void drawRabbit(Canvas c,float x,float y) {
        float s=bunnyH;
        boolean moving=state==3||rightHeld||leftHeld;
        float gait=(float)Math.sin(animTime*(actionHeld?15.5f:10.5f));
        boolean airbornePose=state==4||!onGround || (moving && gait>0);
        float hop=onGround&&moving?Math.max(0,gait)*s*.055f:0;
        y-=hop;
        float spriteW=bunnyW, feetY=state==0?y+s:ground;
        p.setShader(null);p.setStyle(Paint.Style.FILL);p.setAlpha(255);
        p.setColor(Color.argb(onGround?62:32,38,48,48));
        c.drawOval(x+spriteW*.14f,feetY-s*.025f,x+spriteW*.88f,feetY+s*.035f,p);
        if(power==LIGHTNING) {
            p.setColor(Color.rgb(255,236,72));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3*unit);
            c.drawCircle(x+spriteW*.52f,y+s*.53f,s*.54f,p);p.setStyle(Paint.Style.FILL);
        } else if(power==SUPER_JUMP) {
            p.setColor(Color.argb(145,169,113,233));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3*unit);
            c.drawCircle(x+spriteW*.52f,y+s*.53f,s*.54f,p);p.setStyle(Paint.Style.FILL);
        }
        Bitmap sprite=airbornePose?rabbitHop:rabbitIdle;
        if(sprite!=null) {
            float dw=spriteW, dh=s;
            float poseScale=state==3?Math.max(.04f,1f-portalEntryTime/1.7f):(state==4?Math.max(.12f,1f-fallTime*.78f):1f);
            float scaledW=dw*poseScale,scaledH=dh*poseScale;
            RectF dst=new RectF(x+(dw-scaledW)*.5f,y+dh-scaledH,x+(dw+scaledW)*.5f,y+dh);
            if(state==3)p.setAlpha((int)(255*poseScale));
            else if(state==4)p.setAlpha((int)(255*Math.max(.18f,1f-fallTime*.85f)));
            else if(hitCooldown>0 && ((int)(hitCooldown*12)%2==0))p.setAlpha(115);else p.setAlpha(255);
            c.save();
            if(state==4)c.rotate((facingRight?1:-1)*fallTime*170f,x+dw*.5f,y+dh*.7f);
            if(!facingRight)c.scale(-1,1,x+dw*.5f,y+dh*.5f);
            c.drawBitmap(sprite,null,dst,p);
            c.restore();p.setAlpha(255);
        }
    }
    private void drawShot(Canvas c,Shot s) {
        float x=s.x-camera,y=s.y;
        if(s.kind==5) {
            p.setColor(Color.argb(70,255,197,72));c.drawCircle(x,y,12*unit,p);
            p.setShader(new LinearGradient(x-8*unit,y-6*unit,x+7*unit,y+7*unit,Color.rgb(255,242,174),Color.rgb(219,129,38),Shader.TileMode.CLAMP));
            c.drawOval(x-8*unit,y-6*unit,x+8*unit,y+6*unit,p);p.setShader(null);
            p.setColor(Color.rgb(255,247,204));c.drawCircle(x-2*unit,y-2*unit,2*unit,p);
        } else if(s.kind==1) {
            p.setColor(Color.rgb(247,147,40)); RectF r=new RectF(x-10*unit,y-5*unit,x+11*unit,y+6*unit); c.save();c.rotate(-25,x,y);c.drawRoundRect(r,6*unit,6*unit,p);c.restore();
            p.setColor(Color.GREEN); c.drawLine(x+4*unit,y-3*unit,x+12*unit,y-10*unit,p);
        } else if(s.kind==2) {
            // Pena branca com haste e barbas bem marcadas, visível sobre a grama.
            c.save();c.rotate((float)Math.toDegrees(Math.atan2(s.vy,s.vx)),x,y);
            p.setColor(Color.argb(100,39,56,68));Path shadow=new Path();shadow.moveTo(x-13*unit,y+2*unit);shadow.quadTo(x-2*unit,y-10*unit,x+14*unit,y-1*unit);shadow.quadTo(x+3*unit,y+8*unit,x-13*unit,y+2*unit);shadow.close();c.drawPath(shadow,p);
            p.setShader(new LinearGradient(x,y-9*unit,x,y+8*unit,Color.WHITE,Color.rgb(198,221,235),Shader.TileMode.CLAMP));
            Path feather=new Path();feather.moveTo(x-14*unit,y);feather.quadTo(x-4*unit,y-11*unit,x+13*unit,y-2*unit);feather.quadTo(x+5*unit,y+8*unit,x-14*unit,y);feather.close();c.drawPath(feather,p);p.setShader(null);
            p.setColor(Color.rgb(83,135,167));p.setStrokeWidth(1.5f*unit);c.drawLine(x-12*unit,y+1*unit,x+11*unit,y-2*unit,p);
            for(int i=0;i<4;i++){float t=i*.22f;p.setStrokeWidth(1*unit);c.drawLine(x-8*unit+i*4*unit,y,x-10*unit+i*4*unit,y-4*unit,p);c.drawLine(x-7*unit+i*4*unit,y+1*unit,x-8*unit+i*4*unit,y+4*unit,p);}
            c.restore();
        } else if(s.kind==3) {
            // Bola de fogo: halo, rastro e núcleo amarelo para não parecer um projétil branco.
            p.setColor(Color.argb(65,255,68,20));c.drawCircle(x,y,19*unit,p);
            p.setShader(new LinearGradient(x-12*unit,y,x+13*unit,y,Color.rgb(220,39,22),Color.rgb(255,180,40),Shader.TileMode.CLAMP));
            c.drawOval(x-12*unit,y-9*unit,x+13*unit,y+9*unit,p);p.setShader(null);
            p.setColor(Color.rgb(255,237,113));c.drawOval(x-3*unit,y-5*unit,x+9*unit,y+5*unit,p);
            p.setColor(Color.argb(185,255,116,29));Path tail=new Path();tail.moveTo(x-8*unit,y);tail.quadTo(x-18*unit,y-10*unit,x-25*unit,y);tail.quadTo(x-16*unit,y+7*unit,x-8*unit,y);tail.close();c.drawPath(tail,p);
        }
        else if(s.kind==4) {
            float grow=Math.min(1f,s.age/.18f);
            drawSpriteAtBottom(c,spikeSprite,x,terrainY(s.x)+2*unit,60*unit*grow,48*unit*grow);
        } else { p.setColor(Color.rgb(221,239,231));Path t=new Path();t.moveTo(x,y-13*unit);t.lineTo(x+7*unit,y);t.lineTo(x,y+13*unit);t.lineTo(x-7*unit,y);t.close();c.drawPath(t,p); }
    }
    private void drawBurst(Canvas c,Burst b) {
        float x=b.x-camera, y=b.y, radius=(12+b.age*190)*unit;
        p.setColor(Color.argb((int)(200*(1-b.age/.32f)),255,125,45)); c.drawCircle(x,y,radius,p);
        p.setColor(Color.argb((int)(230*(1-b.age/.32f)),255,226,91)); c.drawCircle(x,y,radius*.47f,p);
    }
    private void drawPortal(Canvas c) {
        float y=terrainY(levelLength), x=levelLength-camera-w*.08f;
        if(state==3) { p.setColor(Color.argb(72,214,172,255));c.drawOval(x-48*unit,y-158*unit,x+48*unit,y+9*unit,p); }
        p.setColor(Color.rgb(109,64,180)); c.drawRoundRect(x-30*unit,y-155*unit,x+30*unit,y,30*unit,30*unit,p);
        p.setColor(Color.rgb(154,104,229)); c.drawRoundRect(x-22*unit,y-145*unit,x+22*unit,y,24*unit,24*unit,p);
        p.setColor(Color.rgb(224,248,255)); c.drawOval(x-13*unit,y-130*unit,x+13*unit,y-16*unit,p);
        p.setColor(Color.rgb(255,237,107)); c.drawCircle(x,y-78*unit,6*unit,p);
    }
    private void drawTitle(Canvas c) {
        p.setColor(Color.argb(82,19,42,67));c.drawRect(0,0,w,h,p);
        float l=w*.105f,t=h*.085f,r=w*.895f,b=h*.915f;
        p.setColor(Color.argb(45,28,49,65));c.drawRoundRect(l+5*unit,t+7*unit,r+5*unit,b+7*unit,30*unit,30*unit,p);
        p.setShader(new LinearGradient(l,t,r,b,Color.argb(244,255,255,255),Color.argb(230,239,249,249),Shader.TileMode.CLAMP));
        c.drawRoundRect(l,t,r,b,30*unit,30*unit,p);p.setShader(null);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f*unit);p.setColor(Color.argb(200,255,255,255));c.drawRoundRect(l+2*unit,t+2*unit,r-2*unit,b-2*unit,28*unit,28*unit,p);p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.LEFT);p.setTypeface(android.graphics.Typeface.create("sans-serif-rounded",1));
        p.setColor(Color.rgb(48,144,117));p.setTextSize(13*unit);c.drawRoundRect(l+25*unit,t+24*unit,l+134*unit,t+49*unit,12*unit,12*unit,p);
        p.setColor(Color.WHITE);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(10*unit);c.drawText("AVENTURA 2D",l+79*unit,t+41*unit,p);
        p.setTextAlign(Paint.Align.LEFT);p.setColor(Color.rgb(48,68,76));p.setTextSize(49*unit);c.drawText("COELHITOS",l+25*unit,t+112*unit,p);
        p.setColor(selectedLevel==2?Color.rgb(113,84,162):Color.rgb(76,126,116));p.setTextSize(20*unit);
        c.drawText(selectedLevel==2?"A Caverna dos Cristais":"A Planície do Portal",l+28*unit,t+148*unit,p);
        p.setColor(Color.rgb(93,108,107));p.setTypeface(android.graphics.Typeface.create("sans-serif",0));p.setTextSize(14*unit);
        c.drawText("Escolha uma fase para começar sua jornada.",l+28*unit,t+179*unit,p);
        // Ilustração do mascote no selo do título.
        p.setColor(selectedLevel==2?Color.rgb(203,180,247):Color.rgb(255,210,112));c.drawCircle(r-106*unit,t+105*unit,57*unit,p);
        p.setColor(Color.argb(80,76,95,67));c.drawOval(r-156*unit,t+177*unit,r-42*unit,t+189*unit,p);
        drawRabbit(c,r-147*unit,t+66*unit);

        float cardTop=t+184*unit,cardBottom=t+238*unit;
        float left=l+25*unit,leftRight=w*.5f-8*unit,rightLeft=w*.5f+8*unit,right=r-25*unit;
        int meadow=selectedLevel==1?Color.rgb(229,246,222):Color.rgb(244,248,242);
        int cave=selectedLevel==2?Color.rgb(238,224,255):Color.rgb(244,248,250);
        p.setColor(meadow);c.drawRoundRect(left,cardTop,leftRight,cardBottom,16*unit,16*unit,p);
        p.setColor(cave);c.drawRoundRect(rightLeft,cardTop,right,cardBottom,16*unit,16*unit,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth((selectedLevel==1?2.5f:1f)*unit);p.setColor(selectedLevel==1?Color.rgb(70,158,101):Color.rgb(208,217,214));c.drawRoundRect(left,cardTop,leftRight,cardBottom,16*unit,16*unit,p);
        p.setStrokeWidth((selectedLevel==2?2.5f:1f)*unit);p.setColor(selectedLevel==2?Color.rgb(133,93,195):Color.rgb(208,217,214));c.drawRoundRect(rightLeft,cardTop,right,cardBottom,16*unit,16*unit,p);p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.LEFT);p.setTypeface(android.graphics.Typeface.create("sans-serif-rounded",1));p.setTextSize(14*unit);
        p.setColor(Color.rgb(56,91,67));c.drawText("FASE 1  •  PLANÍCIE",left+14*unit,cardTop+23*unit,p);
        p.setTypeface(android.graphics.Typeface.create("sans-serif",0));p.setTextSize(10*unit);p.setColor(Color.rgb(92,112,98));c.drawText("Grama, morros e portal",left+14*unit,cardTop+41*unit,p);
        p.setTypeface(android.graphics.Typeface.create("sans-serif-rounded",1));p.setTextSize(14*unit);p.setColor(Color.rgb(91,68,128));c.drawText("FASE 2  •  CAVERNA",rightLeft+14*unit,cardTop+23*unit,p);
        p.setTypeface(android.graphics.Typeface.create("sans-serif",0));p.setTextSize(10*unit);p.setColor(Color.rgb(107,91,133));c.drawText("Cristais, rampas e buracos",rightLeft+14*unit,cardTop+41*unit,p);

        float playL=w*.36f,playR=w*.64f,playTop=t+249*unit,playBottom=t+294*unit;
        p.setColor(Color.argb(50,49,88,67));c.drawRoundRect(playL,playTop+4*unit,playR,playBottom+4*unit,17*unit,17*unit,p);
        p.setShader(new LinearGradient(playL,playTop,playL,playBottom,Color.rgb(255,204,85),Color.rgb(248,158,52),Shader.TileMode.CLAMP));
        c.drawRoundRect(playL,playTop,playR,playBottom,17*unit,17*unit,p);p.setShader(null);
        p.setColor(Color.rgb(94,62,40));p.setTextAlign(Paint.Align.CENTER);p.setTypeface(android.graphics.Typeface.create("sans-serif-rounded",1));p.setTextSize(19*unit);
        c.drawText("JOGAR FASE "+selectedLevel,w*.5f,(playTop+playBottom)*.5f+7*unit,p);
        p.setColor(Color.rgb(117,126,120));p.setTypeface(android.graphics.Typeface.create("sans-serif",0));p.setTextSize(9*unit);
        c.drawText("Toque em uma fase para selecioná-la",w*.5f,b-9*unit,p);
    }
    private void drawHud(Canvas c) {
        p.setColor(Color.argb(218,255,255,255));c.drawRoundRect(13*unit,11*unit,232*unit,59*unit,18*unit,18*unit,p);
        p.setColor(Color.rgb(82,93,96));p.setTypeface(android.graphics.Typeface.create("sans-serif-rounded",1));p.setTextAlign(Paint.Align.LEFT);p.setTextSize(10*unit);c.drawText("VIDAS",25*unit,27*unit,p);
        for(int i=0;i<3;i++)drawHeart(c,31*unit+i*25*unit,43*unit,hearts>i?Color.rgb(239,86,105):Color.rgb(205,213,211),7*unit);
        p.setColor(Color.rgb(218,228,222));c.drawLine(111*unit,22*unit,111*unit,49*unit,p);
        drawCarrot(c,132*unit,39*unit,10*unit);
        p.setColor(Color.rgb(71,84,82));p.setTextSize(15*unit);c.drawText("×"+carrots,148*unit,45*unit,p);
        float prog=Math.min(1,playerX/levelLength);
        p.setColor(Color.argb(222,255,255,255));c.drawRoundRect(w*.385f,10*unit,w*.615f,53*unit,16*unit,16*unit,p);
        p.setTextAlign(Paint.Align.CENTER);p.setColor(Color.rgb(79,98,93));p.setTextSize(9*unit);c.drawText(levelNumber==2?"CAVERNA  •  2":"PLANÍCIE  •  1",w*.5f,23*unit,p);
        p.setColor(Color.rgb(224,232,225));c.drawRoundRect(w*.405f,31*unit,w*.595f,38*unit,5*unit,5*unit,p);
        p.setShader(new LinearGradient(w*.405f,0,w*.595f,0,Color.rgb(255,207,88),Color.rgb(244,156,54),Shader.TileMode.CLAMP));c.drawRoundRect(w*.405f,31*unit,w*(.405f+.19f*prog),38*unit,5*unit,5*unit,p);p.setShader(null);
        p.setColor(Color.rgb(96,112,104));p.setTextSize(8*unit);c.drawText("PORTAL",w*.5f,49*unit,p);
        if(state==1) {
            float hl=w-88*unit,hr=w-10*unit,ht=h*.19f,hb=h*.30f;
            p.setColor(Color.argb(210,255,255,255));c.drawRoundRect(hl,ht,hr,hb,13*unit,13*unit,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.3f*unit);p.setColor(Color.argb(220,255,255,255));c.drawRoundRect(hl+1*unit,ht+1*unit,hr-1*unit,hb-1*unit,12*unit,12*unit,p);p.setStyle(Paint.Style.FILL);
            p.setColor(Color.rgb(67,99,95));p.setTextAlign(Paint.Align.CENTER);p.setTypeface(android.graphics.Typeface.create("sans-serif-rounded",1));p.setTextSize(8*unit);
            c.drawText("INÍCIO",(hl+hr)*.5f,(ht+hb)*.5f+3*unit,p);
        }
        if(power!=NONE) {
            p.setColor(Color.argb(222,255,255,255));c.drawRoundRect(w*.79f,11*unit,w*.98f,55*unit,17*unit,17*unit,p);
            int pc=power==FIRE?Color.rgb(236,111,52):power==FEATHER?Color.rgb(78,146,181):power==LIGHTNING?Color.rgb(175,132,34):power==SUPER_JUMP?Color.rgb(139,92,192):Color.rgb(101,138,104);
            p.setColor(pc);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(11*unit);p.setTypeface(android.graphics.Typeface.create("sans-serif-rounded",1));c.drawText(powerName(power).replace("Poder ","").replace("!",""),w*.885f,38*unit,p);
            if(power==LIGHTNING||power==SUPER_JUMP){p.setTextSize(8*unit);c.drawText(String.format(java.util.Locale.US,"%.1fs",Math.max(0,powerTime)),w*.885f,49*unit,p);}
        }
        if(state==2) { p.setColor(Color.argb(176,34,48,63)); c.drawRect(0,0,w,h,p);p.setColor(Color.WHITE);p.setTypeface(android.graphics.Typeface.create("sans-serif-rounded",1));p.setTextSize(31*unit);c.drawText("FASE CONCLUÍDA!",w*.5f,h*.39f,p);p.setTextSize(16*unit);c.drawText("O portal para a próxima aventura está aberto",w*.5f,h*.5f,p);p.setTextSize(13*unit);c.drawText("Toque para jogar novamente",w*.5f,h*.61f,p); }
    }
    private void drawControls(Canvas c) {
        float r=h*.12f, cy=h*.85f, cx=w*.105f;
        p.setColor(Color.argb(55,19,38,54));c.drawCircle(cx,cy+3*unit,r*1.25f,p);
        p.setColor(Color.argb(118,239,249,250));c.drawCircle(cx,cy,r*1.2f,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f*unit);p.setColor(Color.argb(185,255,255,255));c.drawCircle(cx,cy,r*1.2f,p);p.setStyle(Paint.Style.FILL);
        p.setColor(Color.argb(110,105,133,141));p.setTextAlign(Paint.Align.CENTER);p.setTypeface(android.graphics.Typeface.create("sans-serif",1));p.setTextSize(17*unit);
        c.drawText("‹",cx-r*.76f,cy+6*unit,p);c.drawText("›",cx+r*.76f,cy+6*unit,p);c.drawText("⌃",cx,cy-r*.68f+5*unit,p);
        float kx=cx+(rightHeld?1:leftHeld?-1:0)*r*.46f, ky=cy-(upHeld?r*.46f:0);
        p.setColor(Color.argb(65,39,54,59));c.drawCircle(kx,ky+2*unit,r*.48f,p);
        p.setShader(new LinearGradient(kx-r*.4f,ky-r*.5f,kx+r*.4f,ky+r*.5f,Color.WHITE,Color.rgb(192,216,216),Shader.TileMode.CLAMP));c.drawCircle(kx,ky,r*.48f,p);p.setShader(null);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.2f*unit);p.setColor(Color.argb(165,255,255,255));c.drawCircle(kx,ky,r*.48f,p);p.setStyle(Paint.Style.FILL);
        drawButton(c,w*.84f,cy,r*.75f,"AÇÃO",actionHeld?Color.rgb(255,174,75):Color.rgb(255,207,111),true);
        drawButton(c,w*.94f,cy,r*.75f,"PULO",jumpHeld?Color.rgb(104,190,235):Color.rgb(152,216,247),false);
    }
    private void drawButton(Canvas c,float x,float y,float r,String text,int color,boolean action) {
        p.setColor(Color.argb(70,32,47,54));c.drawCircle(x,y+5*unit,r+2*unit,p);
        p.setShader(new LinearGradient(x-r,y-r,x+r,y+r,color,Color.rgb(Math.max(0,Color.red(color)-25),Math.max(0,Color.green(color)-35),Math.max(0,Color.blue(color)-30)),Shader.TileMode.CLAMP));c.drawCircle(x,y,r,p);p.setShader(null);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2*unit);p.setColor(Color.argb(225,255,255,255));c.drawCircle(x,y,r-1*unit,p);p.setStyle(Paint.Style.FILL);
        p.setColor(Color.argb(110,255,255,255));c.drawOval(x-r*.55f,y-r*.68f,x+r*.18f,y-r*.30f,p);
        p.setColor(action?Color.rgb(188,85,42):Color.rgb(48,113,151));
        if(action){Path flame=new Path();flame.moveTo(x,y-r*.34f);flame.quadTo(x+r*.25f,y-r*.04f,x,y+r*.08f);flame.quadTo(x-r*.28f,y-r*.06f,x,y-r*.34f);flame.close();c.drawPath(flame,p);p.setColor(Color.rgb(255,229,144));c.drawCircle(x,y-r*.04f,r*.09f,p);}
        else {Path jump=new Path();jump.moveTo(x-r*.17f,y);jump.lineTo(x,y-r*.2f);jump.lineTo(x+r*.17f,y);jump.moveTo(x,y-r*.2f);jump.lineTo(x,y+r*.12f);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(3*unit);c.drawPath(jump,p);p.setStyle(Paint.Style.FILL);}
        p.setColor(Color.rgb(68,65,62));p.setTypeface(android.graphics.Typeface.create("sans-serif-rounded",1));p.setTextSize(9*unit);p.setTextAlign(Paint.Align.CENTER);c.drawText(text,x,y+r*.58f,p);
    }
    private void drawHeart(Canvas c,float x,float y,int color,float size) {
        p.setColor(color);Path heart=new Path();heart.moveTo(x,y+size*.82f);heart.cubicTo(x-size*1.55f,y-size*.2f,x-size*.85f,y-size*1.2f,x,y-size*.52f);heart.cubicTo(x+size*.85f,y-size*1.2f,x+size*1.55f,y-size*.2f,x,y+size*.82f);heart.close();c.drawPath(heart,p);
    }
    private void drawCarrot(Canvas c,float x,float y,float size) {
        p.setColor(Color.rgb(68,156,84));p.setStrokeWidth(2.1f*unit);c.drawLine(x,y-size*.55f,x-size*.2f,y-size*1.05f,p);c.drawLine(x,y-size*.55f,x+size*.3f,y-size*.95f,p);
        p.setColor(Color.rgb(242,145,43));Path carrot=new Path();carrot.moveTo(x-size*.32f,y-size*.48f);carrot.quadTo(x+size*.4f,y-size*.44f,x+size*.22f,y+size*.32f);carrot.lineTo(x-size*.02f,y+size*.72f);carrot.lineTo(x-size*.12f,y+size*.25f);carrot.close();c.drawPath(carrot,p);
        p.setColor(Color.rgb(255,195,92));c.drawLine(x-size*.13f,y-size*.2f,x+size*.12f,y-size*.17f,p);c.drawLine(x-size*.08f,y+size*.04f,x+size*.10f,y+size*.06f,p);
    }
    private void drawNotice(Canvas c) {
        p.setColor(Color.argb(210,255,255,255));p.setTextAlign(Paint.Align.CENTER);p.setTextSize(19*unit);p.setTypeface(android.graphics.Typeface.create("sans-serif-rounded",1));
        c.drawRoundRect(w*.34f,h*.12f,w*.66f,h*.21f,15*unit,15*unit,p);p.setColor(Color.rgb(73,75,81));c.drawText(notice,w*.5f,h*.18f,p);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        int idx=e.getActionIndex(); int id=e.getPointerId(idx);
        // Em ACTION_POINTER_DOWN, as coordenadas do dedo novo estão no índice de ação;
        // getX()/getY() sem índice devolvem o dedo 0 e quebram o joystick multitoque.
        float x=e.getX(idx),y=e.getY(idx);
        if(state==0) {
            if(e.getAction()==MotionEvent.ACTION_DOWN) {
                if(isMenuCard(x,y,1)) { selectLevel(1); invalidate(); return true; }
                if(isMenuCard(x,y,2)) { selectLevel(2); invalidate(); return true; }
                if(isPlayButton(x,y)) { beginGame(); return true; }
            }
            return true;
        }
        if(state==3||state==4) return true;
        if(state==2) { if(e.getAction()==MotionEvent.ACTION_DOWN) { beginGame(); return true; } return true; }
        switch(e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if(e.getActionMasked()==MotionEvent.ACTION_DOWN && state==1 && isHomeButton(x,y)) { returnToTitle(); invalidate(); return true; }
                if(x<w*.36f && y>h*.52f) {
                    if(stickPointer==-1) { stickPointer=id; updateStick(x,y); }
                    return true;
                }
                if(x>w*.78f && y>h*.58f) {
                    if(x>w*.89f) {
                        if(jumpPointer==-1) { jumpPointer=id; jumpHeld=true; requestJump("button"); }
                    } else if(actionPointer==-1) {
                        actionPointer=id; actionHeld=true; actionDownAt=e.getEventTime();
                    }
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if(stickPointer!=-1) { int si=e.findPointerIndex(stickPointer); if(si>=0) updateStick(e.getX(si),e.getY(si)); }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if(id==stickPointer) { stickPointer=-1; leftHeld=rightHeld=upHeld=false; stickJumpWasDown=false; }
                if(id==actionPointer) {
                    long held=e.getEventTime()-actionDownAt;
                    actionPointer=-1; actionHeld=false;
                    if(held<=420) shoot();
                }
                if(id==jumpPointer) { jumpPointer=-1; jumpHeld=false; }
                break;
            case MotionEvent.ACTION_CANCEL:
                stickPointer=actionPointer=jumpPointer=-1;leftHeld=rightHeld=upHeld=jumpHeld=actionHeld=false;stickJumpWasDown=false;break;
        }
        return true;
    }
    private void updateStick(float x,float y) {
        stickX=x;stickY=y;
        float cx=w*.105f, cy=h*.85f, dx=x-cx, dy=y-cy;
        leftHeld=dx< -h*.045f; rightHeld=dx>h*.045f;
        boolean nextUp=dy< -h*.045f;
        if(nextUp&&!stickJumpWasDown) requestJump("stick");
        stickJumpWasDown=nextUp; upHeld=nextUp;
    }
}

final class Pickup {
    final float x; final int type; final int unused;
    Pickup(float x,int type,int unused){this.x=x;this.type=type;this.unused=unused;}
}
final class Enemy {
    static final int GROUND=0,TAIL=1,FLYING=2,HORNED=3,PLANT_SHOOTER=4,BEETLE=5,MOTH=6;
    float x,hp,phase,attackTime; final int maxHp,type; final boolean big,flying,tail; final float range; boolean dead=false; final float size;
    Enemy(float x,int hp,boolean big,float range,float size){this(x,hp,big,range,size,GROUND);}
    Enemy(float x,int hp,boolean big,float range,float size,int type){this.x=x;this.hp=hp;this.maxHp=hp;this.big=big;this.range=range;this.phase=x*.004f;this.size=size;this.type=type;this.flying=type==FLYING||type==MOTH;this.tail=type==TAIL;this.attackTime=1.7f+Math.abs(phase%1.3f);}
}
final class Hazard {
    final float x,size;final int type;
    Hazard(float x,int type,float size){this.x=x;this.type=type;this.size=size;}
}
final class Decor { final float x; final int kind; final float scale; Decor(float x,int k,float s){this.x=x;kind=k;scale=s;} }
final class Shot {
    float x,y,age=0; final float vx,vy;final int damage,kind;
    Shot(float x,float y,float vx,float vy,int damage,int kind,float age){this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.damage=damage;this.kind=kind;this.age=age;}
}
final class Burst { final float x,y; float age; Burst(float x,float y){this.x=x;this.y=y;} }

final class AudioEngine {
    private volatile boolean running; private volatile int generation; private volatile long pauseUntil;
    private volatile boolean caveTheme;
    private Thread musicThread; private final int rate=22050;
    synchronized void start() {
        if(running)return;
        running=true; int token=++generation;
        musicThread=new Thread(() -> musicLoop(token),"coelhitos-music");musicThread.start();
    }
    void pause() { stopMusic(); }
    synchronized void stopMusic() { running=false; generation++; }
    void stop() { stopMusic(); }
    void setCaveTheme(boolean enabled) { caveTheme=enabled; }
    void pauseMusicBriefly(long durationMs) {
        if(running) pauseUntil=Math.max(pauseUntil,System.currentTimeMillis()+durationMs);
    }
    void lifeLost() { pauseMusicBriefly(680); effect(16); }
    private void musicLoop(int token) {
        AudioTrack track=null;
        try {
            int min=AudioTrack.getMinBufferSize(rate,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);
            track=new AudioTrack(AudioManager.STREAM_MUSIC,rate,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT,Math.max(min,4096),AudioTrack.MODE_STREAM);
            if(track.getState()!=AudioTrack.STATE_INITIALIZED)return;
            track.play();
            short[] buf=new short[2048]; long sample=0;
            int[] meadowNotes={523,0,659,784,0,659,587,0,698,784,0,880,784,0,659,587,523,0,659,784,0,988,880,0,784,659,0,587,659,0,523,0};
            int[] caveNotes={392,0,494,587,0,523,440,0,392,349,0,440,494,0,523,440,392,0,349,392,0,440,494,0,587,523,0,440,392,0,349,0};
            double[] meadowRoots={130.81,110.00,87.31,98.00};
            double[] caveRoots={98.00,87.31,73.42,82.41};
            boolean muted=false;
            while(running && generation==token) {
                int[] notes=caveTheme?caveNotes:meadowNotes;
                double[] roots=caveTheme?caveRoots:meadowRoots;
                long silence=pauseUntil-System.currentTimeMillis();
                if(silence>0) {
                    if(!muted) { track.pause(); track.flush(); muted=true; }
                    Thread.sleep(Math.min(40,silence));
                    continue;
                }
                if(muted) { track.play(); muted=false; }
                for(int i=0;i<buf.length;i++,sample++) {
                    double sec=sample/(double)rate, beat=sec*1.6;
                    int step=(int)Math.floor(beat), noteIndex=step%notes.length, chord=(step/4)%4;
                    double beatPhase=beat-Math.floor(beat), freq=notes[noteIndex];
                    double melody=0;
                    if(freq>0) {
                        double attack=Math.min(1,beatPhase*14), decay=Math.exp(-Math.max(0,beatPhase-.18)*5.2);
                        melody=(Math.sin(2*Math.PI*freq*sec)+.16*Math.sin(4*Math.PI*freq*sec))*attack*decay;
                    }
                    double root=roots[chord];
                    double pad=(Math.sin(2*Math.PI*root*sec)+.72*Math.sin(2*Math.PI*root*1.25*sec)+.54*Math.sin(2*Math.PI*root*1.5*sec));
                    double padPulse=.72+.28*Math.sin(2*Math.PI*.18*sec);
                    double bass=Math.sin(2*Math.PI*(root*.5)*sec)*(.38+.62*Math.exp(-beatPhase*2.6));
                    double kick=beatPhase<.11?Math.sin(2*Math.PI*(76+beatPhase*90)*sec)*Math.exp(-beatPhase*24):0;
                    double mix=melody*.060+pad*padPulse*.018+bass*.045+kick*.012;
                    buf[i]=(short)(Math.max(-.42,Math.min(.42,mix))*32767);
                }
                if(!running || generation!=token || track.write(buf,0,buf.length)<0)break;
            }
        } catch (Exception ignored) {
            // Falha de saída de áudio não deve encerrar o processo do jogo.
        } finally {
            if(track!=null)try{track.pause();track.flush();track.release();}catch(Exception ignored){}
        }
    }
    void effect(int id) {
        final int[][] tunes={{880,1175},{640,960},{420,620,870},{560,0},{240,160},{760,540},{760,980},{780,1170},{170,130},{95,70},{540,780,320},{920,740,550},{310,210,130},{180,140},{115,155,120},{180,210,160},{620,460,340,240},{1046,784,659},{480,380,300,220}};
        int[] seq=tunes[Math.max(0,Math.min(id,tunes.length-1))];
        new Thread(() -> {
            try {
                int n=rate/5; short[] b=new short[n];
                for(int i=0;i<n;i++){double t=i/(double)rate;double f=seq[Math.min(seq.length-1,(int)(t*seq.length*5))];double env=Math.exp(-t*9);b[i]=(short)(Math.sin(2*Math.PI*f*t)*env*6500);}
                AudioTrack a=new AudioTrack(AudioManager.STREAM_MUSIC,rate,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT,n*2,AudioTrack.MODE_STATIC);
                a.write(b,0,b.length);a.play();Thread.sleep(240);a.release();
            } catch(Exception ignored) {}
        },"coelhitos-sfx").start();
    }
}


