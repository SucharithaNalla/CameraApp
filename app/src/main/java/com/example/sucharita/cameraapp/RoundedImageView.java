package com.example.sucharita.cameraapp;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;


public class RoundedImageView extends android.support.v7.widget.AppCompatImageView {


    public RoundedImageView(Context context) {
        super(context);
    }


    public RoundedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.MyImageViewAttr);
        resID_hover = array.getResourceId(R.styleable.MyImageViewAttr_hover_res, -1);
        if (resID_hover != -1) {
            int[] attrsArray = new int[]{
                    android.R.attr.src
            };

            TypedArray ta = context.obtainStyledAttributes(attrs, attrsArray);
            resID = ta.getResourceId(0, View.NO_ID);
            ta.recycle();

            setOnTouchListener(listener_onTouch);
        }

        array.recycle();
    }


    public RoundedImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.MyImageViewAttr);
        resID_hover = array.getResourceId(R.styleable.MyImageViewAttr_hover_res, -1);
        if (resID_hover != -1) {
            int[] attrsArray = new int[]{
                    android.R.attr.src
            };

            TypedArray ta = context.obtainStyledAttributes(attrs, attrsArray);
            resID = ta.getResourceId(0, View.NO_ID);
            ta.recycle();

            setOnTouchListener(listener_onTouch);
        }

        array.recycle();
    }


    int resID, resID_hover;

    OnTouchListener listener_onTouch = new OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // TODO Auto-generated method stub

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    setImageResource(resID_hover);
                    break;

                case MotionEvent.ACTION_MOVE:

                    break;

                case MotionEvent.ACTION_UP:
                    setImageResource(resID);
                    break;

                default:
                    break;
            }


            return false;
        }
    };


    @Override
    protected void onDraw(Canvas canvas) {
        float radius = 10.0f;


        Path clipPath = new Path();
        RectF rect = new RectF(0, 0, this.getWidth(), this.getHeight());
        clipPath.addRoundRect(rect, radius, radius, Path.Direction.CW);
        canvas.clipPath(clipPath);


        super.onDraw(canvas);
    }
}