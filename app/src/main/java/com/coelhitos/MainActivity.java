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
    private final ArrayList<Burst> bursts = new ArrayList<>();
    private final ArrayList<Decor> decor = new ArrayList<>();
    private float w, h, ground, unit, levelLength, camera, playerX, playerY, vy;
    private float normalSpeed, bunnyH, bunnyW, hitCooldown, powerTime, animTime, lastFrame, jumpForwardSpeed;
    private float stepTimer;
    private float portalEntryTime;
    private int hearts = 3, carrots = 0, power = 0, state = 0, collectedChests = 0;
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
        if (state == 1 || state == 3) update(dt);
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
        pickups.clear(); enemies.clear(); hazards.clear(); shots.clear(); bursts.clear(); decor.clear();
        hearts=3; carrots=0; power=NONE; powerTime=0; collectedChests=0; restartPending=false;
        playerX = h*.65f; ground=terrainY(playerX); playerY = ground-bunnyH; vy=0; jumpForwardSpeed=0; onGround=true; facingRight=true;
        camera=0; hitCooldown=0; jumpBufferTime=0;queuedJumpDirection=0;leftHeld=rightHeld=upHeld=jumpHeld=actionHeld=false;
        stickPointer=actionPointer=jumpPointer=-1; stickJumpWasDown=false;
        for(int i=0;i<5;i++) pickups.add(new Pickup(playerX+65*unit + i*112*unit, 0, 0));
        for(int i=0;i<5;i++) pickups.add(new Pickup(levelLength*(.12f+i*.17f), 0, 0));
        for(int i=0;i<3;i++) pickups.add(new Pickup(levelLength*(.24f+i*.25f), 1, 0));
        for(int i=0;i<5;i++) {
            float x=levelLength*(.1f+i*.19f);
            pickups.add(new Pickup(x, 2, 0));
        }
        for(int i=0;i<18;i++) {
            float x=levelLength*(.055f+i*.05f);
            boolean big=i%4==0;
            int type=i%5==2?Enemy.TAIL:Enemy.GROUND;
            enemies.add(new Enemy(x,big?3:1,big,36*unit+(i%4)*14*unit,(big?68:49)*unit,type));
        }
        for(int i=0;i<6;i++) {
            float x=levelLength*(.13f+i*.145f);
            boolean big=i==3;
            enemies.add(new Enemy(x,big?3:1,big,45*unit+(i%3)*10*unit,(big?60:49)*unit,Enemy.FLYING));
        }
        for(int i=0;i<11;i++) {
            float x=levelLength*(.08f+i*.078f);
            // A fase 1 não tem buracos; eles ficam disponíveis para fases futuras.
            int type = i%2==0 ? 0 : 2;
            hazards.add(new Hazard(x, type, (54+ (i%3)*14)*unit));
        }
        for(int i=0;i<80;i++) {
            float x=(i*348f + (i%5)*71f)*unit;
            decor.add(new Decor(x, i%6, 0.65f+(i%4)*.13f));
        }
        startMillis=System.currentTimeMillis();
    }

    private void beginGame() { resetLevel(); state=1; audio.start(); }

    private void update(float dt) {
        if(state==3) { updatePortalEntry(dt); return; }
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
            if (playerY+bunnyH>=landingY) { ground=landingY; playerY=ground-bunnyH; vy=0; onGround=true; jumpForwardSpeed=0; }
        } else { ground=terrainY(playerX); playerY=ground-bunnyH; }
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
        Iterator<Burst> bi=bursts.iterator();
        while(bi.hasNext()) { Burst b=bi.next(); b.age+=dt; if(b.age>.32f) bi.remove(); }
        float portalCenter=levelLength-w*.08f;
        if(dir>0 && playerX+bunnyW*.5f>=portalCenter-bunnyW*.9f) beginPortalEntry();
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
            rightHeld=false; state=2; audio.effect(7); audio.stopMusic(); say("Você entrou no portal!");
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
        float jumpForwardMultiplier=power==SUPER_JUMP?2.15f:1.95f;
        jumpForwardSpeed=normalSpeed*jumpForwardMultiplier*dir;
        Log.d("CoelhitosInput","jump dir="+(int)dir+" source="+source);
        vy=-jumpImpulse(dir); onGround=false; audio.effect(6);
    }
    private float jumpImpulse(float dir) { return power==SUPER_JUMP?h*2.55f:(dir==0?h*1.47f:h*2.4f); }
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
    private void restartAfterDamage() { restartPending=false; beginGame(); say("A fase recomeçou"); }
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
    private void resolvePlayerCollisions(float previousX,float dir) {
        for(Enemy e:enemies) {
            if(e.dead || !enemyOverlapsPlayer(e,playerX,playerY)) continue;
            hurt();
            boolean wasOverlapping=enemyOverlapsPlayer(e,previousX,playerY);
            if(!wasOverlapping || dir>0&&e.x>=previousX || dir<0&&e.x<=previousX) {
                float half=e.size*(e.flying?.57f:.68f);
                if(dir>0) playerX=Math.min(playerX,e.x-half-bunnyW*.87f);
                else if(dir<0) playerX=Math.max(playerX,e.x+half-bunnyW*.13f);
            }
        }
        for(Hazard z:hazards) {
            float floor=terrainY(z.x),left,right,top,bottom=floor;
            if(z.type==1) { left=z.x-z.size*.08f; right=z.x+z.size*1.08f; top=floor-z.size*.58f; }
            else { float half=z.size*(z.type==0?.57f:.52f); left=z.x-half;right=z.x+half;top=floor-z.size*(z.type==0?1.08f:.86f); }
            if(!overlaps(playerX+bunnyW*.13f,playerY+bunnyH*.13f,playerX+bunnyW*.87f,playerY+bunnyH*.98f,left,top,right,bottom)) continue;
            hurt();
            boolean wasOverlapping=overlaps(previousX+bunnyW*.13f,playerY+bunnyH*.13f,previousX+bunnyW*.87f,playerY+bunnyH*.98f,left,top,right,bottom);
            if(!wasOverlapping || dir>0&&z.x>=previousX || dir<0&&z.x<=previousX) {
                if(dir>0) playerX=Math.min(playerX,left-bunnyW*.87f);
                else if(dir<0) playerX=Math.max(playerX,right-bunnyW*.13f);
            }
        }
        if(onGround) { ground=terrainY(playerX); playerY=ground-bunnyH; }
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
        p.setShader(new LinearGradient(0,0,0,h,Color.rgb(72,175,239),Color.rgb(235,249,255),Shader.TileMode.CLAMP));
        c.drawRect(0,0,w,h,p); p.setShader(null);
        drawSun(c);
        drawClouds(c);
        drawHills(c);
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
        for(Burst b:bursts) drawBurst(c,b);
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
    }
    private void drawGround(Canvas c) {
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
    private float terrainY(float worldX) {
        float y=h*.82f;
        y-=hill(worldX,levelLength*.17f,levelLength*.105f,h*.105f);
        y-=hill(worldX,levelLength*.42f,levelLength*.13f,h*.075f);
        y-=hill(worldX,levelLength*.68f,levelLength*.105f,h*.13f);
        y-=hill(worldX,levelLength*.87f,levelLength*.095f,h*.085f);
        y-=ramp(worldX,levelLength*.485f,levelLength*.497f,h*.205f);
        y+=ramp(worldX,levelLength*.514f,levelLength*.526f,h*.205f);
        y-=ramp(worldX,levelLength*.735f,levelLength*.744f,h*.17f);
        y+=ramp(worldX,levelLength*.758f,levelLength*.769f,h*.17f);
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
        if(d.kind==0) {
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
        if(z.type==1) { // asset preparado para fases futuras; a fase 1 não cria buracos.
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
        if(e.flying) drawScarf(c,x,baseY,e.size,wag);
        Bitmap sprite=e.big?enemyLargeSprite:enemySmallSprite;
        float gait=(float)Math.sin(animTime*8f+e.phase);
        c.save();c.translate(x,baseY);c.scale(1f+Math.abs(gait)*.018f,1f-Math.abs(gait)*.025f);c.translate(-x,-baseY);
        float spriteH=drawSpriteAtBottom(c,sprite,x,baseY,e.size*1.86f,e.size*1.34f);c.restore();
        float blink=(animTime+e.phase*.35f)%3.6f;
        if(blink<.13f) {
            float eyeY=baseY-spriteH*.40f;
            p.setColor(e.big?Color.rgb(189,165,223):Color.rgb(148,208,239));p.setStrokeWidth(2.2f*unit);p.setStrokeCap(Paint.Cap.ROUND);
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
        boolean airbornePose=!onGround || (moving && gait>0);
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
            float portalScale=state==3?Math.max(.04f,1f-portalEntryTime/1.7f):1f;
            float scaledW=dw*portalScale,scaledH=dh*portalScale;
            RectF dst=new RectF(x+(dw-scaledW)*.5f,y+dh-scaledH,x+(dw+scaledW)*.5f,y+dh);
            if(state==3)p.setAlpha((int)(255*portalScale));
            else if(hitCooldown>0 && ((int)(hitCooldown*12)%2==0))p.setAlpha(115);else p.setAlpha(255);
            c.save();
            if(!facingRight)c.scale(-1,1,x+dw*.5f,y+dh*.5f);
            c.drawBitmap(sprite,null,dst,p);
            c.restore();p.setAlpha(255);
        }
    }
    private void drawShot(Canvas c,Shot s) {
        float x=s.x-camera,y=s.y;
        if(s.kind==1) {
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
        p.setColor(Color.rgb(76,126,116));p.setTextSize(20*unit);c.drawText("A Planície do Portal",l+28*unit,t+148*unit,p);
        p.setColor(Color.rgb(93,108,107));p.setTypeface(android.graphics.Typeface.create("sans-serif",0));p.setTextSize(14*unit);
        c.drawText("Uma jornada de saltos, poderes e descobertas.",l+28*unit,t+179*unit,p);
        // Insígnia decorativa do mundo inicial.
        p.setColor(Color.rgb(255,210,112));c.drawCircle(r-106*unit,t+105*unit,57*unit,p);
        p.setColor(Color.argb(80,76,95,67));c.drawOval(r-156*unit,t+177*unit,r-42*unit,t+189*unit,p);
        drawRabbit(c,r-147*unit,t+66*unit);
        // CTA principal com hierarquia e contraste claros.
        p.setColor(Color.argb(50,49,88,67));c.drawRoundRect(l+26*unit,t+205*unit,l+240*unit,t+267*unit,19*unit,19*unit,p);
        p.setShader(new LinearGradient(l+26*unit,t+205*unit,l+26*unit,t+263*unit,Color.rgb(255,204,85),Color.rgb(248,158,52),Shader.TileMode.CLAMP));
        c.drawRoundRect(l+26*unit,t+200*unit,l+240*unit,t+260*unit,18*unit,18*unit,p);p.setShader(null);
        p.setColor(Color.rgb(94,62,40));p.setTextAlign(Paint.Align.CENTER);p.setTypeface(android.graphics.Typeface.create("sans-serif-rounded",1));p.setTextSize(21*unit);c.drawText("JOGAR",l+133*unit,t+237*unit,p);
        p.setColor(Color.rgb(244,174,63));c.drawCircle(l+211*unit,t+230*unit,12*unit,p);p.setColor(Color.WHITE);Path arrow=new Path();arrow.moveTo(l+207*unit,t+224*unit);arrow.lineTo(l+215*unit,t+230*unit);arrow.lineTo(l+207*unit,t+236*unit);arrow.close();c.drawPath(arrow,p);
        p.setTextAlign(Paint.Align.LEFT);p.setTypeface(android.graphics.Typeface.create("sans-serif",0));p.setTextSize(12*unit);p.setColor(Color.rgb(97,111,108));
        c.drawText("Explore a planície e alcance o portal.",l+29*unit,t+294*unit,p);
        float hintL=l+270*unit,hintR=r-25*unit,hintW=hintR-hintL;
        float hx1=hintL+hintW/6f,hx2=hintL+hintW/2f,hx3=hintL+hintW*5f/6f;
        p.setColor(Color.rgb(220,231,222));c.drawRoundRect(hintL,t+206*unit,hintR,t+269*unit,15*unit,15*unit,p);
        p.setColor(Color.rgb(56,79,78));p.setTextSize(12*unit);p.setTypeface(android.graphics.Typeface.create("sans-serif-rounded",1));p.setTextAlign(Paint.Align.CENTER);
        c.drawText("MOVER",hx1,t+230*unit,p);c.drawText("PULAR",hx2,t+230*unit,p);c.drawText("AÇÃO",hx3,t+230*unit,p);
        p.setTypeface(android.graphics.Typeface.create("sans-serif",0));p.setTextSize(9*unit);p.setColor(Color.rgb(117,132,126));
        c.drawText("Direcional",hx1,t+249*unit,p);c.drawText("Salta",hx2,t+249*unit,p);c.drawText("Toque atira",hx3,t+249*unit,p);
        p.setTextAlign(Paint.Align.CENTER);p.setColor(Color.rgb(117,132,126));p.setTextSize(10*unit);c.drawText("TOQUE PARA COMEÇAR",w*.5f,b-13*unit,p);
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
        p.setTextAlign(Paint.Align.CENTER);p.setColor(Color.rgb(79,98,93));p.setTextSize(9*unit);c.drawText("PLANÍCIE  •  1",w*.5f,23*unit,p);
        p.setColor(Color.rgb(224,232,225));c.drawRoundRect(w*.405f,31*unit,w*.595f,38*unit,5*unit,5*unit,p);
        p.setShader(new LinearGradient(w*.405f,0,w*.595f,0,Color.rgb(255,207,88),Color.rgb(244,156,54),Shader.TileMode.CLAMP));c.drawRoundRect(w*.405f,31*unit,w*(.405f+.19f*prog),38*unit,5*unit,5*unit,p);p.setShader(null);
        p.setColor(Color.rgb(96,112,104));p.setTextSize(8*unit);c.drawText("PORTAL",w*.5f,49*unit,p);
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
        if(state==0) { if(e.getAction()==MotionEvent.ACTION_DOWN) { beginGame(); return true; } return true; }
        if(state==3) return true;
        if(state==2) { if(e.getAction()==MotionEvent.ACTION_DOWN) { beginGame(); return true; } return true; }
        switch(e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
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
    static final int GROUND=0,TAIL=1,FLYING=2;
    float x,hp,phase; final int maxHp,type; final boolean big,flying,tail; final float range; boolean dead=false; final float size;
    Enemy(float x,int hp,boolean big,float range,float size){this(x,hp,big,range,size,GROUND);}
    Enemy(float x,int hp,boolean big,float range,float size,int type){this.x=x;this.hp=hp;this.maxHp=hp;this.big=big;this.range=range;this.phase=x*.004f;this.size=size;this.type=type;this.flying=type==FLYING;this.tail=type==TAIL;}
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
    private Thread musicThread; private final int rate=22050;
    synchronized void start() {
        if(running)return;
        running=true; int token=++generation;
        musicThread=new Thread(() -> musicLoop(token),"coelhitos-music");musicThread.start();
    }
    void pause() { stopMusic(); }
    synchronized void stopMusic() { running=false; generation++; }
    void stop() { stopMusic(); }
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
            short[] buf=new short[2048]; long sample=0; int[] notes={523,659,784,659,587,698,880,698,523,659,784,988,880,784,659,587};
            boolean muted=false;
            while(running && generation==token) {
                long silence=pauseUntil-System.currentTimeMillis();
                if(silence>0) {
                    if(!muted) { track.pause(); track.flush(); muted=true; }
                    Thread.sleep(Math.min(40,silence));
                    continue;
                }
                if(muted) { track.play(); muted=false; }
                for(int i=0;i<buf.length;i++,sample++) {
                    double sec=sample/(double)rate, beat=sec*2.0;
                    int noteIndex=(int)beat%notes.length, chord=(int)(beat/4)%4;
                    double freq=notes[noteIndex], phase=2*Math.PI*freq*sec;
                    double bass= Math.sin(2*Math.PI*(chord==0?130:chord==1?164:chord==2?196:147)*sec);
                    double melody=Math.sin(phase)+.28*Math.sin(phase*2);
                    double env=.72+.28*Math.sin(2*Math.PI*(beat%1));
                    buf[i]=(short)(Math.max(-1,Math.min(1,(melody*.07+bass*.035)*env))*32767);
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
        final int[][] tunes={{880,1175},{640,960},{420,620,870},{560,0},{240,160},{760,540},{760,980},{780,1170},{170,130},{95,70},{540,780,320},{920,740,550},{310,210,130},{180,140},{115,155,120},{180,210,160},{620,460,340,240}};
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

