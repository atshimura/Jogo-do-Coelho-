package com.coelhitos;

import android.app.Activity;
import android.content.Intent;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Field;
import java.util.ArrayList;

public final class TouchControlsTest extends InstrumentationTestCase {
    private Activity activity;
    private GameView game;
    private long gestureDownTime;

    @Override protected void setUp() throws Exception {
        super.setUp();
        Intent intent = new Intent(getInstrumentation().getTargetContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity = getInstrumentation().startActivitySync(intent);
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

    @Override protected void tearDown() throws Exception {
        if (activity != null) getInstrumentation().runOnMainSync(() -> activity.finish());
        SystemClock.sleep(150);
        super.tearDown();
    }

    public void testJumpButtonFollowsRightAndKeepsJoystickPointer() throws Exception {
        pointerDown(0,180,612);
        pointerDownSecond(1,1203,612,0,180,612);
        assertTrue("PULO must start a forward jump while moving right",fieldFloat("jumpForwardSpeed")>0);
        assertTrue("PULO must not steal the joystick pointer",fieldBoolean("rightHeld"));
        pointerUpSecond(1,1203,612,0,180,612);
        assertTrue("releasing PULO must leave right movement active",fieldBoolean("rightHeld"));
        pointerUp(0,180,612);
    }

    public void testJumpButtonFollowsLeftAndNeutralJumpIsVertical() throws Exception {
        pointerDown(0,80,612);
        pointerDownSecond(1,1203,612,0,80,612);
        assertTrue("PULO must follow movement to the left",fieldFloat("jumpForwardSpeed")<0);
        assertTrue("PULO must not steal the left joystick pointer",fieldBoolean("leftHeld"));
        pointerUpSecond(1,1203,612,0,80,612);
        pointerUp(0,80,612);
    }

    public void testNeutralJumpIsVertical() throws Exception {
        tap(1203,612);
        assertFalse("neutral PULO must start a jump",fieldBoolean("onGround"));
        assertEquals("neutral jump must stay vertical",0f,fieldFloat("jumpForwardSpeed"));
    }

    public void testActionShootsAndKeepsLeftMovement() throws Exception {
        setField("carrots",1);
        pointerDown(0,80,612);
        float before=fieldFloat("playerX");
        pointerDownSecond(1,1075,612,0,80,612);
        assertTrue(fieldBoolean("leftHeld"));
        assertTrue(fieldBoolean("actionHeld"));
        SystemClock.sleep(140);
        float during=fieldFloat("playerX");
        assertTrue("holding AÇÃO with left movement must keep running left",during<before);
        pointerUpSecond(1,1075,612,0,80,612);
        assertEquals("shot must use the leftward movement direction",-1f,fieldFloat("lastShotDirection"));
        assertTrue("releasing AÇÃO must leave joystick movement active",fieldBoolean("leftHeld"));
        SystemClock.sleep(120);
        assertTrue("movement must continue after the shot",fieldFloat("playerX")<during);
        pointerUp(0,80,612);
    }

    public void testActionShootsAndKeepsRightMovement() throws Exception {
        setField("carrots",1);
        pointerDown(0,180,612);
        float before=fieldFloat("playerX");
        pointerDownSecond(1,1075,612,0,180,612);
        assertTrue(fieldBoolean("rightHeld"));
        SystemClock.sleep(140);
        float during=fieldFloat("playerX");
        assertTrue("holding AÇÃO with right movement must keep running right",during>before);
        pointerUpSecond(1,1075,612,0,180,612);
        assertEquals("shot must use the rightward movement direction",1f,fieldFloat("lastShotDirection"));
        assertTrue("releasing AÇÃO must leave joystick movement active",fieldBoolean("rightHeld"));
        SystemClock.sleep(120);
        assertTrue("movement must continue after the shot",fieldFloat("playerX")>during);
        pointerUp(0,180,612);
    }

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
        getInstrumentation().runOnMainSync(()->game.dispatchTouchEvent(event));
        event.recycle();
    }
    private Object field(String name) throws Exception {
        Field f=GameView.class.getDeclaredField(name);f.setAccessible(true);return f.get(game);
    }
    private float fieldFloat(String name) throws Exception { return ((Number)field(name)).floatValue(); }
    private boolean fieldBoolean(String name) throws Exception { return (Boolean)field(name); }
    private void setField(String name,Object value) throws Exception {
        Field f=GameView.class.getDeclaredField(name);f.setAccessible(true);f.set(game,value);
    }
}
