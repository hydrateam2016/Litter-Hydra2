package com.doive.nameless.litter_hydra.widget.live;

import android.content.Context;
import android.support.annotation.IntDef;
import android.support.v4.widget.ViewDragHelper;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import static android.icu.lang.UCharacter.GraphemeClusterBreak.V;

/*
 *  @项目名：  Litter-Hydra2 
 *  @包名：    com.doive.nameless.litter_hydra.widget.live
 *  @文件名:   VideoViewModeHelp
 *  @创建者:   zhong
 *  @创建时间:  2017/5/10 23:31
 *  @描述：
 */
public class VideoViewModeHelp {
    float mDownX = 0;
    float mDownY = 0;
    private View mCurrentView;

    private ViewGroup                  mViewParent;//该View的父控件
    private WindowManager              mWindowManager;//窗口管理器
    private WindowManager.LayoutParams mWMLayoutParams = new WindowManager.LayoutParams();
    private boolean isWmMode;
    public static final int FIX_LAYOUT_MODE  = 0;
    public static final int MOVE_LAYOUT_MODE = 1;
    public static final int ZOOM_LAYOUT_MODE = 1<<2;
    public  @VideoViewModeHelp.LayoutMode
    int mCurrentMode     = FIX_LAYOUT_MODE;

    @IntDef(flag = true,
            value = {FIX_LAYOUT_MODE,
                     MOVE_LAYOUT_MODE,
                     ZOOM_LAYOUT_MODE})
    private @interface LayoutMode { }

    public VideoViewModeHelp(View view) {
        this.mCurrentView = view;
        //初始化对象
        if (mWindowManager == null) {
            mWindowManager = (WindowManager) mCurrentView.getContext().getSystemService(Context.WINDOW_SERVICE);
        }
        if (mViewParent == null) {
            mViewParent = (ViewGroup) mCurrentView.getParent();
        }
        if (mWMLayoutParams == null) {
            mWMLayoutParams = new WindowManager.LayoutParams();
        }
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mCurrentMode = MOVE_LAYOUT_MODE;
                return false;
            }
        });
        ViewDragHelper help = ViewDragHelper.create((ViewGroup)view.getParent(),1.0f,mCallback);
    }

    private ViewDragHelper.Callback mCallback = new ViewDragHelper.Callback() {
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child==mCurrentView;
        }
    };
    public void dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = event.getX();
                mDownY = event.getY();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                //todo 支持双手放大缩小
                break;
            case MotionEvent.ACTION_MOVE:
                float moveX = event.getX();
                float moveY = event.getY();
                //获取移动的距离int
                int movedX = (int) (moveX - mDownX);
                int movedY = (int) (moveY - mDownY);

                handlerTouchEvent(movedX, movedY);
                mDownX = moveX;
                mDownY = moveY;
                break;
            case MotionEvent.ACTION_UP:
                float upX = event.getX();
                float upY = event.getY();
                // TODO: 2017/5/11  越界回弹
                mCurrentMode=FIX_LAYOUT_MODE;
                break;
        }
    }

    /**
     * 处理事件
     * @param movedX
     * @param movedY
     */
    private void handlerTouchEvent(int movedX, int movedY) {
        if (isWmMode) {
            if (mCurrentMode == MOVE_LAYOUT_MODE) {
                mWMLayoutParams.x += movedX;
                mWMLayoutParams.y += movedY;
                mWindowManager.updateViewLayout(mCurrentView, mWMLayoutParams);
            }
            if (mCurrentMode == ZOOM_LAYOUT_MODE) {
                mWMLayoutParams.width += movedX;
                mWMLayoutParams.height += movedY;
                mWindowManager.updateViewLayout(mCurrentView, mWMLayoutParams);
            }
        } else {
            if (mCurrentMode == MOVE_LAYOUT_MODE) {
                mCurrentView.layout(mCurrentView.getLeft()+movedX,mCurrentView.getTop()+movedY,mCurrentView.getRight()+movedX,mCurrentView.getBottom()+movedY);
            }
            if (mCurrentMode == ZOOM_LAYOUT_MODE) {
                ViewGroup.LayoutParams params = mCurrentView.getLayoutParams();
                params.width =mCurrentView.getWidth()+movedX;
                params.height =mCurrentView.getHeight()+movedY;
                mCurrentView.setLayoutParams(params);
            }
        }
    }

    /**
     * 切换到窗口模式
     * @return
     */
    public boolean switchSuspendedWindowMode() {
        boolean isSuccess = false;
        //获取窗口管理器
        if (!isWmMode) {
            //配置参数
            mWMLayoutParams.gravity = Gravity.TOP | Gravity.START;
            mWMLayoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
            mWMLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            //获取当前view在屏幕的绝对坐标
            int[] location = getViewLocationOnScreen();
            mWMLayoutParams.x = location[0];
            mWMLayoutParams.y = location[1];
            //设置大小
            mWMLayoutParams.width = mCurrentView.getLayoutParams().width;
            mWMLayoutParams.height = mCurrentView.getLayoutParams().height;
            //替换view
            mViewParent.removeView(mCurrentView);
            mWindowManager.addView(mCurrentView, mWMLayoutParams);
            //更改标志位
            isSuccess = true;
            isWmMode = true;
        }

        return isSuccess;
    }

    private int[] getViewLocationOnScreen() {
        int[] location = new int[2];
        mCurrentView.getLocationOnScreen(location);
        return location;
    }

    /**
     * 切换回layout模式
     * @return
     */
    public boolean switchLayoutMode() {
        boolean isSuccess = false;
        if (isWmMode) {
            mWindowManager.removeViewImmediate(mCurrentView);
            mViewParent.addView(mCurrentView, 0, mCurrentView.getLayoutParams());
            isSuccess = true;
            isWmMode = false;
        }
        return isSuccess;
    }
}