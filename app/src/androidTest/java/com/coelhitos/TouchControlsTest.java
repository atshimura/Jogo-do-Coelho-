package com.coelhitos;

import android.app.Activity;
import android.content.Intent;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class TouchControlsTest {
    private Activity activity;
    private GameView game;
    private long gestureDownTime;

    @Before public void setUp() throws Exception {
        Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity = InstrumentationRegistry.getInstrumentation().startActivitySync(intent);
        for (int i=0;i<60;i++) {
            ViewGroup content = (ViewGroup)activity.findViewById(android.R.id.content);
            if (content != null && content.getChildCount() > 0 && content.getChildAt(0).getWidth() > 0) {
                game = (GameView)content.getChildAt(0);
                break;
            }
            SystemClock.sleep(50);
        }
        assertNotNull("GameView was not laid out",game);
        tap(640,360); // start the level
    }

    @After public void tearDown() throws Exception {
        if (activity != null) InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> activity.finish());
        SystemClock.sleep(150);
    }

    @Test
    public void testJumpButtonFollowsRightAndKeepsJoystickPointer() throws Exception {
        pointerDown(0,180,612);
        pointerDownSecond(1,1203,612,0,180,612);
        assertTrue("PULO must start a forward jump while moving right",fieldFloat("jumpForwardSpeed")>0);
        assertTrue("PULO must not steal the joystick pointer",fieldBoolean("rightHeld"));
        pointerUpSecond(1,1203,612,0,180,612);
        assertTrue("releasing PULO must leave right movement active",fieldBoolean("rightHeld"));
        pointerUp(0,180,612);
    }

    @Test
    public void testJumpButtonFollowsLeftAndNeutralJumpIsVertical() throws Exception {
        pointerDown(0,80,612);
        pointerDownSecond(1,1203,612,0,80,612);
        assertTrue("PULO must follow movement to the left",fieldFloat("jumpForwardSpeed")<0);
        assertTrue("PULO must not steal the left joystick pointer",fieldBoolean("leftHeld"));
        pointerUpSecond(1,1203,612,0,80,612);
        pointerUp(0,80,612);
    }

    @Test
    public void testNeutralJumpIsVertical() throws Exception {
        tap(1203,612);
        assertFalse("neutral PULO must start a jump",fieldBoolean("onGround"));
        assertEquals("neutral jump must stay vertical",0f,fieldFloat("jumpForwardSpeed"),0f);
    }

    @Test
    public void testActionShootsAndKeepsLeftMovement() throws Exception {
        setField("carrots",1);
        setField("playerX",game.getWidth()*.5f);
        pointerDown(0,80,612);
        float before=fieldFloat("playerX");
        pointerDownSecond(1,1075,612,0,80,612);
        assertTrue(fieldBoolean("leftHeld"));
        assertTrue(fieldBoolean("actionHeld"));
        SystemClock.sleep(120);
        float during=fieldFloat("playerX");
        assertTrue("holding AÇÃO with left movement must keep running left: before="+before+" after="+during,during<before);
        pointerUpSecond(1,1075,612,0,80,612);
        assertEquals("shot must use the leftward movement direction",-1f,fieldFloat("lastShotDirection"),0f);
        assertTrue("releasing AÇÃO must leave joystick movement active",fieldBoolean("leftHeld"));
        SystemClock.sleep(120);
        assertTrue("movement must continue after the shot",fieldFloat("playerX")<during);
        pointerUp(0,80,612);
    }

    @Test
    public void testActionShootsAndKeepsRightMovement() throws Exception {
        setField("carrots",1);
        setField("playerX",game.getWidth()*.5f);
        pointerDown(0,180,612);
        float before=fieldFloat("playerX");
        pointerDownSecond(1,1075,612,0,180,612);
        assertTrue(fieldBoolean("rightHeld"));
        SystemClock.sleep(120);
        float during=fieldFloat("playerX");
        assertTrue("holding AÇÃO with right movement must keep running right",during>before);
        pointerUpSecond(1,1075,612,0,180,612);
        assertEquals("shot must use the rightward movement direction",1f,fieldFloat("lastShotDirection"),0f);
        assertTrue("releasing AÇÃO must leave joystick movement active",fieldBoolean("rightHeld"));
        SystemClock.sleep(120);
        assertTrue("movement must continue after the shot",fieldFloat("playerX")>during);
        pointerUp(0,180,612);
    }

    @Test
    public void testLongActionHoldRunsWithoutFiring() throws Exception {
        setField("carrots",1);
        pointerDown(0,180,612);
        pointerDownSecond(1,1075,612,0,180,612);
        SystemClock.sleep(500);
        pointerUpSecond(1,1075,612,0,180,612);
        ArrayList<?> shots=(ArrayList<?>)field("shots");
        assertEquals("long AÇÃO press is for running, not firing",0,shots.size());
        assertTrue(fieldBoolean("rightHeld"));
        pointerUp(0,180,612);
    }

    @Test
    public void testPowerChancesAndDurations() throws Exception {
        Method roll=GameView.class.getDeclaredMethod("powerForRoll",int.class);roll.setAccessible(true);
        int[] counts=new int[6];
        for(int i=0;i<100;i++)counts[(Integer)roll.invoke(game,i)]++;
        assertArrayEquals(new int[]{0,25,13,25,20,17},counts);
        Method duration=GameView.class.getDeclaredMethod("powerDuration",int.class);duration.setAccessible(true);
        assertEquals(8f,(Float)duration.invoke(game,2),0f);
        assertEquals(20f,(Float)duration.invoke(game,5),0f);
        assertEquals(0f,(Float)duration.invoke(game,1),0f);
        setField("power",1);
        Method shoot=GameView.class.getDeclaredMethod("shoot");shoot.setAccessible(true);shoot.invoke(game);
        Shot thorn=(Shot)((ArrayList<?>)field("shots")).get(0);
        assertEquals("Espinho must grow from the ground rather than fall",4,thorn.kind);
        assertEquals("growing Espinho stays at ground height",0f,thorn.vy,0f);
    }

    @Test
    public void testSupersaltoMakesDirectionalJumpHigherAndLonger() throws Exception {
        setField("power",5);
        setField("powerTime",20f);
        pointerDown(0,180,612);
        pointerDownSecond(1,1203,612,0,180,612);
        assertTrue("Supersalto must increase forward travel",fieldFloat("jumpForwardSpeed")>fieldFloat("normalSpeed")*1.8f);
        Method impulse=GameView.class.getDeclaredMethod("jumpImpulse",float.class);impulse.setAccessible(true);
        assertEquals("Supersalto must increase jump height",game.getHeight()*2f,(Float)impulse.invoke(game,1f),.1f);
        pointerUpSecond(1,1203,612,0,180,612);
        pointerUp(0,180,612);
        SystemClock.sleep(220);
        assertTrue("Supersalto must remain active during its duration",fieldFloat("powerTime")<20f&&fieldInt("power")==5);
    }

    @Test
    public void testDirectionalJumpClearsSpikesFireAndBothEnemies() throws Exception {
        clearAndJumpOver(false,0,0);
        clearAndJumpOver(false,1,0);
        clearAndJumpOver(false,2,0);
        clearAndJumpOver(true,0,1);
        clearAndJumpOver(true,0,3);
    }

    @Test
    public void testRabbitWalksIntoPortalBeforeLevelComplete() throws Exception {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            try {
                float portalCenter=fieldFloat("levelLength")-game.getWidth()*.08f;
                float bunnyW=fieldFloat("bunnyW");
                setField("playerX",portalCenter-bunnyW*1.25f);
                setField("camera",Math.max(0,fieldFloat("playerX")-game.getWidth()*.28f));
                Method enter=GameView.class.getDeclaredMethod("beginPortalEntry");enter.setAccessible(true);enter.invoke(game);
            } catch(Exception e) { throw new RuntimeException(e); }
        });
        assertEquals("approaching the portal starts a visible entry sequence",3,fieldInt("state"));
        float start=fieldFloat("playerX");
        Method step=GameView.class.getDeclaredMethod("updatePortalEntry",float.class);step.setAccessible(true);
        for(int i=0;i<30 && fieldInt("state")==3;i++) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
                try { step.invoke(game,.1f); } catch(Exception e) { throw new RuntimeException(e); }
            });
        }
        assertTrue("rabbit should walk forward into the portal",fieldFloat("playerX")>start);
        assertEquals("level completion comes after portal entry",2,fieldInt("state"));
        assertFalse("touch movement is cleared during entry",fieldBoolean("rightHeld"));
    }

    @Test
    public void testLevelHasMoreEnemyTypesAndStraightRamp() throws Exception {
        ArrayList<?> enemies=(ArrayList<?>)field("enemies");
        assertTrue("phase should contain a larger enemy group",enemies.size()>=24);
        int flying=0,tail=0;
        for(Object value:enemies){Enemy e=(Enemy)value;if(e.flying)flying++;if(e.tail)tail++;}
        assertTrue("phase should include scarf wearing flying enemies",flying>=5);
        assertTrue("phase should include enemies with moving tails",tail>=3);
        Method terrain=GameView.class.getDeclaredMethod("terrainY",float.class);terrain.setAccessible(true);
        float level=fieldFloat("levelLength"),height=game.getHeight();
        float start=(Float)terrain.invoke(game,level*.485f),end=(Float)terrain.invoke(game,level*.497f);
        assertTrue("the straight uphill segment should visibly climb",start-end>height*.14f);
    }

    @Test
    public void testEnemyAndSpikeCollisionsStopRabbit() throws Exception {
        Method terrain=GameView.class.getDeclaredMethod("terrainY",float.class);terrain.setAccessible(true);
        Method resolve=GameView.class.getDeclaredMethod("resolvePlayerCollisions",float.class,float.class);resolve.setAccessible(true);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            try {
                ((ArrayList<?>)field("enemies")).clear();((ArrayList<?>)field("hazards")).clear();
                setField("power",0);setField("hearts",3);setField("hitCooldown",0f);
                float start=game.getWidth()*.34f,wid=fieldFloat("bunnyW"),unit=fieldFloat("unit");
                setField("playerX",start);setField("playerY",((Float)terrain.invoke(game,start))-fieldFloat("bunnyH"));
                ((ArrayList<Enemy>)field("enemies")).add(new Enemy(start+wid*1.2f,1,false,0,52*unit));
                setField("playerX",start+wid*.55f);resolve.invoke(game,start,1f);
                assertEquals("colliding with an enemy costs one heart",2,fieldInt("hearts"));
                assertTrue("enemy collision blocks forward movement",fieldFloat("playerX")<start+wid*.55f);
                ((ArrayList<?>)field("enemies")).clear();setField("hearts",3);setField("hitCooldown",0f);setField("playerX",start);
                ((ArrayList<Hazard>)field("hazards")).add(new Hazard(start+wid*1.35f,0,54*unit));
                setField("playerX",start+wid*.55f);resolve.invoke(game,start,1f);
                assertEquals("spike collision costs one heart",2,fieldInt("hearts"));
                assertTrue("spike collision blocks forward movement",fieldFloat("playerX")<start+wid*.55f);
            } catch(Exception e) { throw new RuntimeException(e); }
        });
    }

    private void clearAndJumpOver(boolean enemy,int type,int hp) throws Exception {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            try {
                ((ArrayList<?>)field("hazards")).clear();
                ((ArrayList<?>)field("enemies")).clear();
                ((ArrayList<?>)field("pickups")).clear();
                ((ArrayList<?>)field("shots")).clear();
                float target=fieldFloat("levelLength")*.31f;
                float size=enemy?(hp>1?136f:98f):164f;
                float radius=enemy?fieldFloat("bunnyW")*.65f:size*.5f+fieldFloat("bunnyW")*.35f;
                float speed=fieldFloat("normalSpeed")*1.55f;
                setField("playerX",target-radius-speed*.38f-(enemy?0:fieldFloat("bunnyW")*.5f));
                setField("onGround",true);setField("vy",0f);setField("jumpForwardSpeed",0f);
                setField("hitCooldown",0f);setField("hearts",3);setField("power",0);setField("powerTime",0f);
                if(enemy)((ArrayList<Enemy>)field("enemies")).add(new Enemy(target,hp,hp>1,0f,size));
                else ((ArrayList<Hazard>)field("hazards")).add(new Hazard(target,type,size));
            } catch(Exception e) { throw new RuntimeException(e); }
        });
        SystemClock.sleep(60);
        pointerDown(0,180,612);
        pointerDownSecond(1,1203,612,0,180,612);
        SystemClock.sleep(1000);
        int hearts=fieldInt("hearts");
        pointerUpSecond(1,1203,612,0,180,612);
        pointerUp(0,180,612);
        assertEquals("timed forward jump must clear "+(enemy?"enemy":"hazard type="+type),3,hearts);
    }

    private void tap(float x,float y) {
        pointerDown(0,x,y);
        pointerUp(0,x,y);
    }
    private void pointerDown(int id,float x,float y) {
        send(MotionEvent.ACTION_DOWN,new int[]{id},new float[][]{{x,y}});
    }
    private void pointerUp(int id,float x,float y) {
        send(MotionEvent.ACTION_UP,new int[]{id},new float[][]{{x,y}});
    }
    private void pointerDownSecond(int id,float x,float y,int firstId,float firstX,float firstY) {
        send(MotionEvent.ACTION_POINTER_DOWN | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                new int[]{firstId,id},new float[][]{{firstX,firstY},{x,y}});
    }
    private void pointerUpSecond(int id,float x,float y,int firstId,float firstX,float firstY) {
        send(MotionEvent.ACTION_POINTER_UP | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                new int[]{firstId,id},new float[][]{{firstX,firstY},{x,y}});
    }
    private void send(int action,int[] ids,float[][] xy) {
        long now=SystemClock.uptimeMillis();
        if((action & MotionEvent.ACTION_MASK)==MotionEvent.ACTION_DOWN)gestureDownTime=now;
        MotionEvent.PointerProperties[] properties=new MotionEvent.PointerProperties[ids.length];
        MotionEvent.PointerCoords[] coords=new MotionEvent.PointerCoords[ids.length];
        for(int i=0;i<ids.length;i++) {
            properties[i]=new MotionEvent.PointerProperties();properties[i].id=ids[i];properties[i].toolType=MotionEvent.TOOL_TYPE_FINGER;
            coords[i]=new MotionEvent.PointerCoords();coords[i].x=xy[i][0];coords[i].y=xy[i][1];coords[i].pressure=1;coords[i].size=1;
        }
        MotionEvent event=MotionEvent.obtain(gestureDownTime,now,action,ids.length,properties,coords,0,0,1,1,0,0,InputDevice.SOURCE_TOUCHSCREEN,0);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->game.dispatchTouchEvent(event));
        event.recycle();
    }
    private Object field(String name) throws Exception {
        Field f=GameView.class.getDeclaredField(name);f.setAccessible(true);return f.get(game);
    }
    private float fieldFloat(String name) throws Exception { return ((Number)field(name)).floatValue(); }
    private int fieldInt(String name) throws Exception { return ((Number)field(name)).intValue(); }
    private boolean fieldBoolean(String name) throws Exception { return (Boolean)field(name); }
    private void setField(String name,Object value) throws Exception {
        Field f=GameView.class.getDeclaredField(name);f.setAccessible(true);f.set(game,value);
    }
}
