package com.zidian.parkmapapp.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.zidian.parkmapapp.R;

import java.util.List;

public class ParkMapView extends FrameLayout implements ViewTreeObserver.OnGlobalLayoutListener, ScaleGestureDetector.OnScaleGestureListener {

    private static final String TAG = "ParkMapView";
    //最小倍数
    private float minScale;
    //最大倍数
    private float maxScale;
    //默认倍数
    private float defalutScale;
    //双击手势一次放大倍数
    private float midScale;


    private Context context;
    private int mapWidth;
    private int mapHeight;
    private int mapRes;
    private ImageView mapView;
    private List<Marker> markerBeans;
    private List<ImageView> markerViews;
    private Matrix scaleMatrix;
    private boolean isAutoScaling;
    private boolean isPicLoaded;

    private OnMarkerClickListner onMarkerClickListner;
    //缩放手势
    private ScaleGestureDetector scaleGestureDetector;
    //双击手势
    private GestureDetector gestureDetector;


    public ParkMapView(Context context) {
        this(context, null);
    }

    public ParkMapView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ParkMapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        initAttributes(attrs);
        initMapView();
    }

    private void initAttributes(AttributeSet attrs) {
        if (attrs == null) {
            return;
        }
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ParkMapView);
        mapRes = a.getResourceId(R.styleable.ParkMapView_map_background, R.drawable.bg_test);
        a.recycle();
    }

    private void initMapView() {
        //缩放系数初始化
        defalutScale = 1.0f;
        minScale = 0.5f;
        maxScale = 5.0f;
        midScale = 2.0f;

        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mapView = new AppCompatImageView(context);
        mapView.setImageResource(mapRes);
        mapView.setLayoutParams(params);
        addView(mapView);

        scaleMatrix = new Matrix();

        scaleGestureDetector = new ScaleGestureDetector(context, this);

        gestureDetector = new GestureDetector(context, new SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (!isAutoScaling) {//如果不在自动缩放
                    isAutoScaling = true;
                    float x = e.getX();//双击触点x坐标
                    float y = e.getY();//双击触点y坐标
                    float scale = getDrawableScale();
                    if (scale < midScale) {//当前缩放比例小于一级缩放比例
                        //一级放大
                        post(new AutoScaleTask(midScale, x, y));
                    } else if (scale >= midScale && scale < maxScale) {//当前缩放比例在一级缩放和二级缩放比例之间
                        //二级放大
                        post(new AutoScaleTask(maxScale, x, y));
                    } else if (scale == maxScale) {//当前缩放比例等于二级缩放比例
                        //缩小至自适应view比例
                        post(new AutoScaleTask(defalutScale, x, y));
                    } else {
                        isAutoScaling = false;
                    }
                }
                return super.onDoubleTap(e);
            }
        });
    }


    public void onChanged(RectF rectF) {
        if (markerBeans == null) {
            return;
        }
        float pWidth = rectF.width();
        float pHeight = rectF.height();
        float pLeft = rectF.left;
        float pTop = rectF.top;

        Marker marker = null;
        for (int i = 0, size = markerBeans.size(); i < size; i++) {

            marker = markerBeans.get(i);

            int left = roundValue(pLeft + pWidth * marker.getPercentX() - marker.getWidth());
            int top = roundValue(pTop + pHeight * marker.getPercentY() - marker.getHeight());
            int right = roundValue(pLeft + pWidth * marker.getPercentX() + marker.getWidth());
            int bottom = roundValue(pTop + pHeight * marker.getPercentY() + marker.getHeight());

            marker.setDrawX(left);
            marker.setDrawY(top);
        }
    }

    public void setMarkers(List<Marker> markers) {
        this.markerBeans = markers;
    }

    public void setOnMarkerClickListner(OnMarkerClickListner l) {
        this.onMarkerClickListner = l;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float scaleFactor = detector.getScaleFactor();
        //当前图片已缩放的值（如果onScale第一次被调用，scale就是自适应后的缩放值：defalutScale）
        float scale = getDrawableScale();
        //当前缩放值在最大放大值以内且手势检测缩放因子为缩小手势(小于1)，或当前缩放值在最小缩小值以内且缩放因子为放大手势，允许缩放
        if (scale <= maxScale && scaleFactor < 1 || scale >= minScale && scaleFactor > 1) {
            //进一步考虑即将缩小后的缩放比例(scale*scaleFactor)低于规定minScale-maxScale范围的最小值minScale
            if (scale * scaleFactor < minScale && scaleFactor < 1) {
                //强制锁定缩小后缩放比例为minScale（scale*scaleFactor=minScale）
                scaleFactor = minScale / scale;
            }
            //进一步考虑即将放大后的缩放比例(scale*scaleFactor)高于规定minScale-maxScale范围的最大值maxScale
            if (scale * scaleFactor > maxScale && scaleFactor > 1) {
                //强制锁定放大后缩放比例为maxScale（scale*scaleFactor=maxScale）
                scaleFactor = maxScale / scale;
            }
            //设定缩放值和缩放位置，这里缩放位置便是手势焦点的位置
            scaleMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());

            //检查即将缩放后造成的留空隙和图片不居中的问题，及时调整缩放参数
            checkBoderAndCenter();

            //执行缩放
            mapView.setImageMatrix(scaleMatrix);
            onChanged(getMatrixRect());
        }
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        //当前缩放值
        float scale = getDrawableScale();
        //当前缩放值小于自适应缩放缩放比例，即图片小于View宽高
        if (scale < defalutScale) {
            post(new AutoScaleTask(defalutScale, getWidth() * 1f / 2, getHeight() * 1f));
        }
    }

    @Override
    public void onGlobalLayout() {
        if (!isPicLoaded) {
            Drawable drawable = mapView.getDrawable();

            if (null == drawable) {//图片不存在就继续监听
                return;
            }

            mapWidth = drawable.getIntrinsicWidth();
            mapHeight = drawable.getIntrinsicHeight();

            isPicLoaded = true;//图片存在，已加载完成，停止监听
            //获取图片固有的宽高（不是指本身属性:分辨率，因为android系统在加载显示图片前可能对其压缩）
            int iWidth = drawable.getIntrinsicWidth();
            int iHeight = drawable.getIntrinsicHeight();

            //获取当前View(ImageView)的宽高，即父View给予的宽高
            int width = getWidth();
            int height = getHeight();

            //TODO log
            Log.e("TAG", "mapWidth:" + mapWidth);
            Log.e("TAG", "mapHeight:" + mapHeight);
            Log.e("TAG", "width:" + width);
            Log.e("TAG", "height:" + height);

            //调整默认缩放比例，使初始刚好显示整张图片的大小
            if (iWidth >= width && iHeight <= height) {
                defalutScale = width * 1f / iWidth;
            } else if (iWidth < width && iHeight > height) {
                defalutScale = height * 1f / iHeight;
            } else {
                defalutScale = Math.max(width * 1f / iWidth, height * 1f / iHeight);
            }

            //先将图片移动到View中心位置
            scaleMatrix.postTranslate((width - iWidth) * 1f / 2, (height - iHeight) * 1f / 2);
            //再对图片从View的中心点缩放
            scaleMatrix.postScale(defalutScale, defalutScale, width * 1f / 2, height * 1f / 2);
            //执行偏移和缩放
            mapView.setImageMatrix(scaleMatrix);
            onChanged(getMatrixRect());

            //根据当前图片的缩放情况，重新调整图片的最大最小缩放值
            maxScale *= defalutScale;
            midScale *= defalutScale;
            minScale *= defalutScale;

            //TODO log
            Log.e("TAG", "defalutScale:" + defalutScale);
            Log.e("TAG", "maxScale:" + maxScale);
            Log.e("TAG", "midScale:" + midScale);
            Log.e("TAG", "minScale:" + minScale);
            Log.e("TAG", "scale:" + getDrawableScale());

        }
    }

    public interface OnMarkerClickListner {
        void onClick(View view, int position);
    }


    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        drawPath();
        drawRipples();
        drawMarkers();
    }

    private void drawPath() {

    }

    private void drawRipples() {

    }

    private void drawMarkers() {

    }

    private int roundValue(float value) {
        return Math.round(value);
    }

    //双击自动缩放
    private class AutoScaleTask implements Runnable {
        float targetScale;
        float x;
        float y;
        static final float TMP_AMPLIFY = 1.06f;
        static final float TMP_SHRINK = 0.94f;
        float tmpScale;

        AutoScaleTask(float targetScale, float x, float y) {
            this.targetScale = targetScale;
            this.x = x;
            this.y = y;
            //当前缩放值小于目标缩放值，目标是放大图片
            if (getDrawableScale() < targetScale) {
                //设定缩放梯度为放大梯度
                tmpScale = TMP_AMPLIFY;
            } else {  //当前缩放值小于(等于可以忽略)目标缩放值，目标是缩小图片
                //设定缩放梯度为缩小梯度
                tmpScale = TMP_SHRINK;
            }
        }

        @Override
        public void run() {
            //循环放大或缩小
            scaleMatrix.postScale(tmpScale, tmpScale, x, y);
            //检查即将缩放后造成的留空隙和图片不居中的问题，及时调整缩放参数
            checkBoderAndCenter();
            mapView.setImageMatrix(scaleMatrix);
            onChanged(getMatrixRect());
            //当前缩放值
            float scale = getDrawableScale();
            Log.e("TAG", "scale:" + scale);
            //如果tmpScale>1即放大任务状态，且当前缩放值还是小于目标缩放值或
            // tmpScale<1即缩小任务状态，且当前缩放值还是大于目标缩放值就继续执行缩放任务
            if (tmpScale > 1 && scale < targetScale || scale > targetScale && tmpScale < 1) {
                post(this);
            } else {//缩放的略微过头了,需要强制设定为目标缩放值
                tmpScale = targetScale / scale;
                scaleMatrix.postScale(tmpScale, tmpScale, x, y);
                checkBoderAndCenter();
                mapView.setImageMatrix(scaleMatrix);
                onChanged(getMatrixRect());
                isAutoScaling = false;
            }
        }
    }

    int lastX;
    int lastY;
    int lastCount;

    private void moveByTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE://手势移动
                RectF rect = getMatrixRect();
                if (rect.width() <= getWidth() && rect.height() <= getHeight()) {
                    return;
                }
                int x = 0;
                int y = 0;
                int pointerCount = event.getPointerCount();
                for (int i = 0; i < pointerCount; i++) {
                    x += event.getX(i);
                    y += event.getY(i);
                }
                //所有触点中点
                x /= pointerCount;
                y /= pointerCount;

                //重置触点数
                if (lastCount != pointerCount) {
                    lastX = x;
                    lastY = y;
                    lastCount = pointerCount;
                }
                int deltaX = x - lastX;
                int deltaY = y - lastY;

                if (isCanDrag(deltaX, deltaY)) {

                    //横向不用移动
                    if (rect.width() <= getWidth()) {
                        deltaX = 0;
                    }
                    //纵向不用移动
                    if (rect.height() <= getHeight()) {
                        deltaY = 0;
                    }

                    scaleMatrix.postTranslate(deltaX, deltaY);
                    checkBoderAndCenter();
                    mapView.setImageMatrix(scaleMatrix);
                    onChanged(getMatrixRect());
                }

                lastX = x;
                lastY = y;
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                lastCount = 0;
                break;
        }
    }


    //保证图片居中
    private void checkBoderAndCenter() {
        RectF rect = getMatrixRect();
        int width = getWidth();
        int height = getHeight();

        float deltaX = 0;
        float deltaY = 0;

        //图片宽度大于View宽
        if (rect.width() >= width) {
            //图片左边坐标大于0，即左边有空隙
            if (rect.left > 0) {
                //向左移动rect.left个单位到View最左边,rect.left=0
                deltaX = -rect.left;
            }
            //图片右边坐标小于width，即右边有空隙
            if (rect.right < width) {
                //向右移动width - rect.left个单位到View最右边,rect.right=width
                deltaX = width - rect.right;
            }
        }
        //图片高度大于View高
        if (rect.height() >= height) {
            //图片上面坐标大于0，即上面有空隙
            if (rect.top > 0) {
                //向上移动rect.top个单位到View最上边,rect.top=0
                deltaY = -rect.top;
            }
            //图片下面坐标小于height，即下面有空隙
            if (rect.bottom < height) {
                //向下移动height - rect.bottom个单位到View最下边,rect.bottom=height
                deltaY = height - rect.bottom;
            }
        }

        //图片宽度小于View宽
        if (rect.width() < width) {
            //计算需要移动到X方向View中心的距离
            deltaX = width * 1f / 2 - rect.right + rect.width() * 1f / 2;
        }

        //图片高度小于View高度
        if (rect.height() < height) {
            //计算需要移动到Y方向View中心的距离
            deltaY = height * 1f / 2 - rect.bottom + rect.height() * 1f / 2;
        }

        scaleMatrix.postTranslate(deltaX, deltaY);
    }

    //根据缩放矩阵获取RectF
    private RectF getMatrixRect() {
        RectF rect = new RectF();
        rect.set(0, 0, mapWidth, mapHeight);
        scaleMatrix.mapRect(rect);

        return rect;
    }


    //获取当前缩放值
    private float getDrawableScale() {
        float[] values = new float[9];
        scaleMatrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        if (!isAutoScaling) {
            moveByTouchEvent(event);
        }
        return true;
    }

    //计算偏移量是否达到最小滑动距离
    private boolean isCanDrag(int deltaX, int deltaY) {
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY) >= ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //view加载完成的初始化
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeOnGlobalLayoutListener(this);
    }

}