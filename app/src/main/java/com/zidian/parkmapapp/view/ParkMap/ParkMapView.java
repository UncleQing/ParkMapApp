package com.zidian.parkmapapp.view.ParkMap;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;


import com.zidian.parkmapapp.R;

import java.util.ArrayList;
import java.util.List;

import static com.zidian.parkmapapp.view.ParkMap.MarkerBean.TYPE_CAR;
import static com.zidian.parkmapapp.view.ParkMap.MarkerBean.TYPE_Other;


public class ParkMapView extends FrameLayout implements ViewTreeObserver.OnGlobalLayoutListener,
        ScaleGestureDetector.OnScaleGestureListener {
    private final static String TAG = "ParkMapView";
    private float SCALE_MID = 2.0f; //中间放大比例值系数，双击一次的放大值
    private float SCALE_MAX = 4.0f;
    private float SCALE_MIN = 0.5f;
    private float SCALE_ADAPTIVE = 1.0f;//自适应ViewGroup(或屏幕)缩放比例值
    //默认地图尺寸
    private static final float MAP_HEIGHT = 1735;
    private static final float MAP_WIDTH = 1080;

    private Context context;
    private int mapHeight;
    private int mapWidth;
    private int mapRes;

    private ImageView mapLayer;

    private Matrix mScaleMatrix;//缩放矩阵
    private ScaleGestureDetector mScaleGestureDetector;//缩放手势探测测器
    private GestureDetector mGestureDetector;//手势探测器
    private boolean isAutoScaling = false;//是否处于自动缩放中,用于是否响应双击手势的flag
    private int mTouchSlop;
    private boolean isPicLoaded = false;//图片是否已加载

    //上一次触点中心坐标
    int mLastX;
    int mLastY;
    //上一次拖动图片的触点数（手指数）
    int mLastPointCount;

    private List<MarkerBean> markers;
    private List<PathBean> paths;
    private OnSpaceClickListenner spaceClickListner;

    //路径画笔
    private Paint linePaint;
    private Path linePath;

    //marker画笔
    private Paint markerPaint;
    private Bitmap bm_car;
    private Bitmap bm_other;
    private Bitmap bm_ripple;
    //水波纹
    private Paint ripplePaint;
    private float rippleScale;
    private int rippleAlpha;

    //车位画笔
    private Paint spacePaint;
    private RectF spaceRect;
    private List<ParkSpaceBean> spaceCoordinates;
    private boolean isDownSpace;
    private int currentSpaceID = -1;

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
        initData();
        initMapLayer();
    }

    private void initAttributes(AttributeSet attrs) {
        if (attrs == null) {
            return;
        }
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ParkMapView);
        mapRes = a.getResourceId(R.styleable.ParkMapView_map_background, R.drawable.bg_test);
        a.recycle();
    }

    private void initData() {
        markers = new ArrayList<>();
        paths = new ArrayList<>();

        mScaleMatrix = new Matrix();
        mScaleGestureDetector = new ScaleGestureDetector(context, this);
        mGestureDetector = initGestureDetector(context);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        linePaint = new Paint();
        linePaint.setStrokeWidth(2.0f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);
        linePaint.setColor(context.getResources().getColor(R.color.green));

        linePath = new Path();

        markerPaint = new Paint();
        markerPaint.setStrokeWidth(3.0f);
        markerPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        markerPaint.setAntiAlias(true);

        ripplePaint = new Paint();
        ripplePaint.setStrokeWidth(3.0f);
        ripplePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        ripplePaint.setAntiAlias(true);

        bm_other = BitmapFactory.decodeResource(getResources(), R.drawable.location);
        bm_car = BitmapFactory.decodeResource(getResources(), R.drawable.ic_car);
        bm_ripple = BitmapFactory.decodeResource(getResources(), R.drawable.bg_car);

        spacePaint = new Paint();
        spacePaint.setStrokeWidth(5.0f);
        spacePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        spacePaint.setAntiAlias(true);
        spacePaint.setColor(context.getResources().getColor(R.color.transparent_primary));

        //space init
        spaceCoordinates = new ArrayList<>();
        ParkSpaceBean parkSpaceBean0 = new ParkSpaceBean(1, 0.334f, 0.119f, 0.352f, 0.141f);
        ParkSpaceBean parkSpaceBean1 = new ParkSpaceBean(2, 0.316f, 0.119f, 0.334f, 0.141f);
        ParkSpaceBean parkSpaceBean2 = new ParkSpaceBean(3, 0.214f, 0.119f, 0.316f, 0.141f);
        ParkSpaceBean parkSpaceBean3 = new ParkSpaceBean(4, 0.306f, 0.857f, 0.324f, 0.877f);
        ParkSpaceBean parkSpaceBean4 = new ParkSpaceBean(5, 0.288f, 0.857f, 0.306f, 0.877f);
        ParkSpaceBean parkSpaceBean5 = new ParkSpaceBean(6, 0.269f, 0.857f, 0.287f, 0.877f);
        spaceCoordinates.add(parkSpaceBean0);
        spaceCoordinates.add(parkSpaceBean1);
        spaceCoordinates.add(parkSpaceBean2);
        spaceCoordinates.add(parkSpaceBean3);
        spaceCoordinates.add(parkSpaceBean4);
        spaceCoordinates.add(parkSpaceBean5);
    }

    public void testMarker(){
        List<MarkerBean> markerBeanList = new ArrayList<>();

        MarkerBean markerBean3 = new MarkerBean();
        markerBean3.setCoordinate(new Coordinate(0.306f, 0.127f));
        markerBean3.setType(TYPE_CAR);
        markerBean3.setRotation(0f);
        markerBean3.setId(1);
        markerBeanList.add(markerBean3);

        setMarkers(markerBeanList);
        invalidate();
    }

    @SuppressLint("WrongConstant")
    private void initMapLayer() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -1);
        mapLayer = new ImageView(context);
        mapLayer.setLayoutParams(params);
        mapLayer.setImageResource(mapRes);
        mapLayer.setScaleType(ImageView.ScaleType.MATRIX);
        addView(mapLayer);

        //不同dpi目录下取得值不同
        mapHeight = mapLayer.getDrawable().getIntrinsicHeight();
        mapWidth = mapLayer.getDrawable().getIntrinsicWidth();

        //水波纹动画
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(1.0f, 2.0f);
        valueAnimator.setDuration(1000);
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                rippleScale = (float) animation.getAnimatedValue();
                float s = rippleScale - 1.0f;
                rippleAlpha = 255 - (int) (255 * s);
                if (!isAutoScaling) ParkMapView.this.invalidate();
            }
        });
        valueAnimator.setRepeatMode(ValueAnimator.INFINITE);
        valueAnimator.setRepeatCount(ValueAnimator.INFINITE);
        valueAnimator.start();
    }

    /**
     * 初始化手势探测器
     *
     * @param context
     * @return GestureDetector
     */
    private GestureDetector initGestureDetector(Context context) {
        GestureDetector.SimpleOnGestureListener listner = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (!isAutoScaling) {//如果不在自动缩放
                    isAutoScaling = true;
                    float x = e.getX();//双击触点x坐标
                    float y = e.getY();//双击触点y坐标
                    float scale = getDrawableScale();
                    if (scale < SCALE_MID) {//当前缩放比例小于一级缩放比例
                        //一级放大
                        post(new AutoScaleTask(SCALE_MID, x, y));
                    } else if (scale >= SCALE_MID && scale < SCALE_MAX) {//当前缩放比例在一级缩放和二级缩放比例之间
                        //二级放大
                        post(new AutoScaleTask(SCALE_MAX, x, y));
                    } else if (scale == SCALE_MAX) {//当前缩放比例等于二级缩放比例
                        //缩小至自适应view比例
                        post(new AutoScaleTask(SCALE_ADAPTIVE, x, y));
                    } else {
                        isAutoScaling = false;
                    }
                }
                return super.onDoubleTap(e);
            }
        };
        return new GestureDetector(context, listner);
    }

    /**
     * 获取当前已经缩放的比例
     * 因为x方向和y方向比例相同，所以只返回x方向的缩放比例即可
     */
    private float getDrawableScale() {
        float[] values = new float[9];
        mScaleMatrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        //返回为true，则缩放手势事件往下进行，否则到此为止，即不会执行onScale和onScaleEnd方法
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if (mapLayer.getDrawable() == null) {
            return true;
        }
        //缩放因子(即将缩放的值)
        float scaleFactor = detector.getScaleFactor();
        //当前图片已缩放的值（如果onScale第一次被调用，scale就是自适应后的缩放值：SCALE_ADAPTIVE）
        float scale = getDrawableScale();
        //当前缩放值在最大放大值以内且手势检测缩放因子为缩小手势(小于1)，或当前缩放值在最小缩小值以内且缩放因子为放大手势，允许缩放
        if (scale <= SCALE_MAX && scaleFactor < 1 || scale >= SCALE_MIN && scaleFactor > 1) {
            //进一步考虑即将缩小后的缩放比例(scale*scaleFactor)低于规定SCALE_MIN-SCALE_MAX范围的最小值SCALE_MIN
            if (scale * scaleFactor < SCALE_MIN && scaleFactor < 1) {
                //强制锁定缩小后缩放比例为SCALE_MIN（scale*scaleFactor=SCALE_MIN）
                scaleFactor = SCALE_MIN / scale;
            }
            //进一步考虑即将放大后的缩放比例(scale*scaleFactor)高于规定SCALE_MIN-SCALE_MAX范围的最大值SCALE_MAX
            if (scale * scaleFactor > SCALE_MAX && scaleFactor > 1) {
                //强制锁定放大后缩放比例为SCALE_MAX（scale*scaleFactor=SCALE_MAX）
                scaleFactor = SCALE_MAX / scale;
            }
            //设定缩放值和缩放位置，这里缩放位置便是手势焦点的位置
            mScaleMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());

            //检查即将缩放后造成的留空隙和图片不居中的问题，及时调整缩放参数
            checkBoderAndCenter();
            //执行缩放
            mapLayer.setImageMatrix(mScaleMatrix);
            updateMarkersFromScale();
        }
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        Drawable drawable = mapLayer.getDrawable();
        if (drawable == null) return;
        //当前缩放值
        float scale = getDrawableScale();
        //当前缩放值小于自适应缩放缩放比例，即图片小于View宽高
        if (scale < SCALE_ADAPTIVE) {
            post(new AutoScaleTask(SCALE_ADAPTIVE, getWidth() * 1f / 2, getHeight() * 1f));
        }
    }

    @Override
    public void onGlobalLayout() {
        if (!isPicLoaded) {
            Drawable drawable = mapLayer.getDrawable();
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

            //对比图片宽高和当前View的宽高，针对性的缩放
            if (iWidth >= width && iHeight < height) {//固宽大于view宽，固高小于view高
                SCALE_ADAPTIVE = width * 1f / iWidth;
            } else if (iHeight >= height && iWidth < width) {//固高大于view高，固宽小于view宽
                SCALE_ADAPTIVE = height * 1f / iHeight;
            } else {//固宽和固高都大于或都小于View的宽高，
                SCALE_ADAPTIVE = Math.max(width * 1f / iWidth, height * 1f / iHeight);
                //只取对宽和对高之间最小的缩放比例值（这里有别于查看大图的处理方式）
            }

            //先将图片移动到View中心位置
            //再对图片从View的中心点缩放

            float dx = (width - iWidth) * 1f / 2;
            float dy = (height - iHeight) * 1f / 2;
            float sx = SCALE_ADAPTIVE;
            float sy = SCALE_ADAPTIVE;
            float px = width * 1f / 2;
            float py = height * 1f / 2;
            mScaleMatrix.postTranslate(dx, dy);
            mScaleMatrix.postScale(sx, sy, px, py);
            //执行偏移和缩放
            mapLayer.setImageMatrix(mScaleMatrix);
            updateMarkersFromScale();

            //根据当前图片的缩放情况，重新调整图片的最大最小缩放值
            SCALE_MAX *= SCALE_ADAPTIVE;
            SCALE_MID *= SCALE_ADAPTIVE;
            SCALE_MIN *= SCALE_ADAPTIVE;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //订阅布局监听
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        //取消订阅
        getViewTreeObserver().removeOnGlobalLayoutListener(this);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (mScaleGestureDetector != null) {
            //绑定缩放手势探测器,由其处理touch事件
            mScaleGestureDetector.onTouchEvent(event);
        }
        if (mGestureDetector != null) {
            //绑定手势探测器,由其处理touch事件
            mGestureDetector.onTouchEvent(event);
        }
        if (!isAutoScaling) {
            //绑定touch事件，处理移动图片逻辑
            moveByTouchEvent(event);
        }
        //停车位选择
        if (!isAutoScaling) {
            clickByTouchEvent(event);
        }
        return true;
    }

    /**
     * 通过Touch事件选择停车位
     *
     * @param event
     */
    float downX;
    float downY;

    private void clickByTouchEvent(MotionEvent event) {
        int pointerCount = event.getPointerCount();//获取触点数（手指数）
        if (pointerCount > 1) {
            return;
        }

        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = x;
                downY = y;
                isDownSpace = isSpaceRect(x, y) > -1;
                break;
            case MotionEvent.ACTION_MOVE:
                int deltaX = (int) (downX - x);
                int deltaY = (int) (downY - y);
                if (isCanDrag(deltaX, deltaY)) {
                    isDownSpace = false;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (!isDownSpace) return;
                int id = isSpaceRect(x, y);
                if (id > 0 && id != currentSpaceID) {
                    ParkSpaceBean bean = spaceCoordinates.get(id - 1);
                    float left = bean.getLtCoordinate().getX();
                    float top = bean.getLtCoordinate().getY();
                    float right = bean.getRbCoordinate().getX();
                    float bottom = bean.getRbCoordinate().getY();
                    spaceRect = new RectF(left, top, right, bottom);
                    currentSpaceID = bean.getId();
                    if (spaceClickListner != null) {
                        spaceClickListner.onClick(currentSpaceID);
                    }
                    invalidate();
                }
                break;
        }
    }

    /**
     * 通过Touch事件移动图片
     *
     * @param event
     */
    private void moveByTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE://手势移动
                RectF rect = getMatrixRect();
                if (rect.width() <= getWidth() && rect.height() <= getHeight()) {
                    //图片宽高小于等于View宽高，即图片可以完全显示于屏幕中，那就没必要拖动了
                    return;
                }
                //计算多个触点的中心坐标
                int x = 0;
                int y = 0;
                int pointerCount = event.getPointerCount();//获取触点数（手指数）
                for (int i = 0; i < pointerCount; i++) {
                    x += event.getX(i);
                    y += event.getY(i);
                }
                //得到最终的中心坐标
                x /= pointerCount;
                y /= pointerCount;

                //如果触点数（手指数）发生变化，需要重置上一次中心坐标和数量的参考值
                if (mLastPointCount != pointerCount) {
                    mLastX = x;
                    mLastY = y;
                    mLastPointCount = pointerCount;
                }
                int deltaX = x - mLastX;//X方向的位移
                int deltaY = y - mLastY;//Y方向的位移
                //如果可以拖拽
                if (isCanDrag(deltaX, deltaY)) {

                    //图片宽小于等于view宽，则X方向不需要移动
                    if (rect.width() <= getWidth()) {
                        deltaX = 0;
                    }
                    //图片高小于等于view高，则Y方向不需要移动
                    if (rect.height() <= getHeight()) {
                        deltaY = 0;
                    }
                    //完成缩放
                    mScaleMatrix.postTranslate(deltaX, deltaY);
                    checkBoderAndCenter();
                    mapLayer.setImageMatrix(mScaleMatrix);
                    updateMarkersFromScale();
                }
                //交换中心坐标值，作为下次移动事件的参考值
                mLastX = x;
                mLastY = y;
                break;
            case MotionEvent.ACTION_CANCEL://取消
            case MotionEvent.ACTION_UP://释放
                mLastPointCount = 0;//触点数置零，便于下次判断是否重置mLastX和mLastY
                break;
        }
    }

    /**
     * 点击区域是否是停车位区域
     *
     * @param deltaX
     * @param deltaY
     */
    private int isSpaceRect(float deltaX, float deltaY) {
        if (spaceCoordinates == null) {
            return -1;
        }

        if (!isPicLoaded) return -1;

        for (int i = 0; i < spaceCoordinates.size(); i++) {
            ParkSpaceBean bean = spaceCoordinates.get(i);
            float left = bean.getLtCoordinate().getX();
            float top = bean.getLtCoordinate().getY();
            float right = bean.getRbCoordinate().getX();
            float bottom = bean.getRbCoordinate().getY();

            boolean x = deltaX < right && deltaX > left;
            boolean y = deltaY < bottom && deltaY > top;

            if (x && y) return bean.getId();
        }

        return -1;
    }


    /**
     * 是否可以移动图片
     *
     * @param deltaX
     * @param deltaY
     */
    private boolean isCanDrag(int deltaX, int deltaY) {
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY) >= mTouchSlop;
    }

    /**
     * 自动缩放任务
     */
    private class AutoScaleTask implements Runnable {
        float targetScale;//目标缩放值
        float x;//缩放焦点的x坐标
        float y;//缩放焦点的y坐标
        static final float TMP_AMPLIFY = 1.06f;//放大梯度
        static final float TMP_SHRINK = 0.94f;//缩小梯度
        float tmpScale = 1f;//缩小梯度

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
            //设定缩放参数
            mScaleMatrix.postScale(tmpScale, tmpScale, x, y);
            //检查即将缩放后造成的留空隙和图片不居中的问题，及时调整缩放参数
            checkBoderAndCenter();
            mapLayer.setImageMatrix(mScaleMatrix);
            updateMarkersFromScale();
            //当前缩放值
            float scale = getDrawableScale();

            //如果tmpScale>1即放大任务状态，且当前缩放值还是小于目标缩放值或
            // tmpScale<1即缩小任务状态，且当前缩放值还是大于目标缩放值就继续执行缩放任务
            if (tmpScale > 1 && scale < targetScale || scale > targetScale && tmpScale < 1) {
                post(this);
            } else {//缩放的略微过头了,需要强制设定为目标缩放值
                tmpScale = targetScale / scale;
                mScaleMatrix.postScale(tmpScale, tmpScale, x, y);
                checkBoderAndCenter();
                mapLayer.setImageMatrix(mScaleMatrix);
                updateMarkersFromScale();
                isAutoScaling = false;
            }
        }
    }

    /**
     * 处理缩放和移动后图片边界与屏幕有间隙或者不居中的问题
     */
    private void checkBoderAndCenter() {
        RectF rect = getMatrixRect();
        int width = getWidth();
        int height = getHeight();

        float deltaX = 0;//X轴方向偏移量
        float deltaY = 0;//Y轴方向偏移量

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
        //图片高度大于View高，同理
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
        //设置移动参数
        mScaleMatrix.postTranslate(deltaX, deltaY);
    }

    /**
     * 根据当前图片矩阵变换成的四个角的坐标，即left,top,right,bottom
     *
     * @return
     */
    private RectF getMatrixRect() {
        RectF rect = new RectF();
        Drawable drawable = mapLayer.getDrawable();
        if (drawable != null) {
            rect.set(0.0f, 0.0f, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        }
        mScaleMatrix.mapRect(rect);
        return rect;
    }


    private void updateMarkersFromScale() {
        if (markers.size() == 0) {
            Log.e(TAG, "Markers is null");
            return;
        }
        RectF rectF = getMatrixRect();
        float pWidth = rectF.width();//地图宽度
        float pHeight = rectF.height();//地图高度
        float pLeft = rectF.left;//地图左边x坐标
        float pTop = rectF.top;//地图顶部y坐标

        float scaleH = pHeight / mapHeight;
        float scaleW = pWidth / mapWidth;

        //marker
        for (int i = 0, size = markers.size(); i < size; i++) {

            MarkerBean marker = markers.get(i);

            //缩放宽高
            if (marker.getType() == TYPE_CAR) {
                marker.setWidth(scaleW * bm_car.getWidth());
                marker.setHeight(scaleH * bm_car.getHeight());
                marker.setRippleWidth(scaleW * bm_ripple.getWidth());
                marker.setRippleHeight(scaleH * bm_ripple.getWidth());
            } else {
                marker.setWidth(scaleW * bm_other.getWidth());
                marker.setHeight(scaleH * bm_other.getHeight());
            }

            /* 计算marker显示的矩形坐标*/
//            float left = pLeft + pWidth * marker.getCoordinate().getX() - marker.getWidth() * 1f / 2;
            float left = pLeft + pWidth * marker.getCoordinate().getX();
//            float top = pTop + pHeight * marker.getCoordinate().getY() - marker.getHeight() * 1f / 2;
            float top = pTop + pHeight * marker.getCoordinate().getY();
            float right = pLeft + pWidth * marker.getCoordinate().getX() + marker.getWidth() * 1f / 2;
            float bottom = pTop + pHeight * marker.getCoordinate().getY() + marker.getHeight() * 1f / 2;

            //marker 画布坐标
            Coordinate canvasCoor = marker.getCanvas_coordinate();
            if (canvasCoor == null) {
                canvasCoor = new Coordinate(left, top);
            } else {
                canvasCoor.setX(left);
                canvasCoor.setY(top);
            }
            marker.setCanvas_coordinate(canvasCoor);

        }

        //path
        for (int i = 0; paths != null && i < paths.size(); i++) {
            PathBean pathBean = paths.get(i);
            float x = pLeft + pWidth * pathBean.getCoordinate().getX();
            float y = pTop + pHeight * pathBean.getCoordinate().getY();
            pathBean.setCanvas_coordinate(new Coordinate(x, y));
        }

        //update space rect
        for (int i = 0; spaceCoordinates != null && i < spaceCoordinates.size(); i++) {
            ParkSpaceBean bean = spaceCoordinates.get(i);
            float ltxs = bean.getLtCoordinateScale().getX();
            float ltys = bean.getLtCoordinateScale().getY();
            float rbxs = bean.getRbCoordinateScale().getX();
            float rbys = bean.getRbCoordinateScale().getY();

            float ltx = pLeft + pWidth * ltxs;
            float lty = pTop + pHeight * ltys;
            float rbx = pLeft + pWidth * rbxs;
            float rby = pTop + pHeight * rbys;

            bean.setLtCoordinate(new Coordinate(ltx, lty));
            bean.setRbCoordinate(new Coordinate(rbx, rby));
        }
        //update current space rect
        if (spaceRect != null) {
            ParkSpaceBean bean = spaceCoordinates.get(currentSpaceID - 1);
            spaceRect.set(bean.getLtCoordinate().getX(), bean.getLtCoordinate().getY(), bean.getRbCoordinate().getX()
                    , bean.getRbCoordinate().getY());
        }

        invalidate();
    }


    @Override
    protected void dispatchDraw(Canvas canvas) {
        //bg
        super.dispatchDraw(canvas);
        //space
        drawSpace(canvas);
        //path
        drawPath(canvas);
        //marker ripple
        drawRipple(canvas);
        //marker
        drawMarkers(canvas);
    }

    //选择车位
    private void drawSpace(Canvas canvas) {
        if (spaceRect != null)
            canvas.drawRect(spaceRect, spacePaint);
    }

    private void drawPath(Canvas canvas) {
        if (paths == null || paths.size() < 2) {
            return;
        }
        linePath.reset();
        for (int i = 0; i < paths.size(); i++) {
            PathBean bean = paths.get(i);
            Coordinate coordinate = bean.getCanvas_coordinate();
            if (coordinate != null) {
                if (i == 0) {
                    linePath.moveTo(coordinate.getX(), coordinate.getY());
                } else {
                    linePath.lineTo(coordinate.getX(), coordinate.getY());
                }
            }

        }
        canvas.drawPath(linePath, linePaint);
    }

    private void drawRipple(Canvas canvas) {
        if (markers.size() < 1) {
            return;
        }

        for (int i = 0; i < markers.size(); i++) {
            MarkerBean bean = markers.get(i);
            if (bean.getType() == TYPE_CAR) {
                float x = bean.getCanvas_coordinate().getX();
                float y = bean.getCanvas_coordinate().getY();

                Matrix matrix = new Matrix();
                matrix.setScale(getDrawableScale() * rippleScale, getDrawableScale() * rippleScale);
                matrix.postTranslate(x - (rippleScale) * (bean.getRippleWidth() / 2),
                        y - (rippleScale) * (bean.getRippleHeight() / 2));
                ripplePaint.setAlpha(rippleAlpha);
                canvas.drawBitmap(bm_ripple, matrix, ripplePaint);

                Matrix matrix2 = new Matrix();
                matrix2.setScale(getDrawableScale(), getDrawableScale());
                matrix2.postTranslate(x - bean.getRippleWidth() / 2, y - bean.getRippleHeight() / 2);
                ripplePaint.setAlpha(128);
                canvas.drawBitmap(bm_ripple, matrix2, ripplePaint);
                break;
            }
        }
    }

    private void drawMarkers(Canvas canvas) {
        if (markers.size() < 1) {
            return;
        }
        for (int i = 0; i < markers.size(); i++) {
            MarkerBean bean = markers.get(i);
            float x = bean.getCanvas_coordinate().getX();
            float y = bean.getCanvas_coordinate().getY();
            Matrix matrix = new Matrix();
            matrix.setScale(getDrawableScale(), getDrawableScale());
            matrix.postTranslate(x - bean.getWidth() / 2, y - bean.getHeight() / 2);
            matrix.postRotate(bean.getRotation(), x, y);
            canvas.drawBitmap(getMarkerBitmap(bean.getType()), matrix, markerPaint);
        }
    }

    private Bitmap getMarkerBitmap(int type) {
        if (type == TYPE_Other) {
            return bm_other;
        } else {
            return bm_car;
        }
    }

    public void setMarkers(List<MarkerBean> markers) {
        this.markers = markers;
    }

    public void updateLocalPosition(int x, int y, int yaw) {
        float xp = x * 1.0f / MAP_WIDTH;
        float yp = y * 1.0f / MAP_HEIGHT;

        //clear map
        if (x == -1 && y == -1 && yaw == -1) {
            markers.clear();
            invalidate();
            return;
        }

        if (markers.size() == 0) {
            MarkerBean bean = new MarkerBean();
            bean.setType(TYPE_CAR);
            bean.setId(TYPE_CAR);
            bean.setRotation(yaw);
            bean.setCoordinate(new Coordinate(xp, yp));

            markers.add(bean);
        } else {
            for (MarkerBean bean : markers) {
                if (bean.getType() == TYPE_CAR) {
                    bean.setRotation(yaw);
                    bean.getCoordinate().setX(xp);
                    bean.getCoordinate().setY(yp);
                    break;
                }
            }
        }
        updateMarkersFromScale();
    }

    public void showObstacle() {
        if (markers.size() == 1) {
            MarkerBean carBean = markers.get(0);
            if (carBean.getType() == TYPE_CAR) {
                MarkerBean markerBean4 = new MarkerBean();
                markerBean4.setCoordinate(new Coordinate(carBean.getCoordinate().getX() + 0.01f,
                        carBean.getCoordinate().getY() - 0.01f));
                markerBean4.setType(TYPE_Other);
                markerBean4.setId(TYPE_Other);
                markers.add(markerBean4);
            }
        }
        updateMarkersFromScale();
    }

    public void hideObstacle() {
        if (markers.size() < 2) return;
        for (int i = 0; i < markers.size(); i++) {
            MarkerBean bean = markers.get(i);
            if (bean.getType() == TYPE_Other) {
                markers.remove(bean);
                break;
            }
        }
        updateMarkersFromScale();
    }

    public void setPaths(List<PathBean> pathList) {
        paths.clear();
        if (pathList != null && pathList.size() > 0) {
            for (int i = 0; i < pathList.size(); i++) {
                PathBean coordinate = pathList.get(i);
                float xp = coordinate.getCoordinate().getX() *  1.0f / MAP_WIDTH;
                float yp = coordinate.getCoordinate().getY() * 1.0f / MAP_HEIGHT;
                PathBean bean = new PathBean();
                bean.setCoordinate(new Coordinate(xp, yp));
                bean.setIndex(i);

                paths.add(bean);
            }
        }
        updateMarkersFromScale();
    }

    public void setSpaceClickListner(OnSpaceClickListenner spaceClickListner) {
        this.spaceClickListner = spaceClickListner;
    }

    public interface OnSpaceClickListenner {
        void onClick(int id);
    }
}
