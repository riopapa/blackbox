package com.urrecliner.blackbox;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.OverScroller;

import java.util.ArrayList;
import java.util.List;

public class WheelView extends View {
	public static final float DEFAULT_INTERVAL_FACTOR = 1.2f;
	public static final float DEFAULT_MARK_RATIO = 0.7f;

	private TextPaint mMarkHighPaint, mMarkNormPaint, mOutlinePaint;
	private int mCenterIndex = -1;

	private int mMarkHighColor, mMarkNormColor;
	private int mMarkColor;

	private int mHeight;
	private List<String> mItems;
	private float mIntervalFactor = DEFAULT_INTERVAL_FACTOR;
	private float mMarkRatio = DEFAULT_MARK_RATIO;

	private int mMarkCount;
	private float mCursorSize;
	private int mViewScopeSize;

	// scroll control args ---- start
	private OverScroller mScroller;
	private float mMaxOverScrollDistance;
	private RectF mContentRectF;
	private boolean mFling = false;
	private float mCenterTextSize, mNormalTextSize;
	private float mTopSpace, mBottomSpace;
	private float mIntervalDis;

	private int mLastSelectedIndex = -1;
	private int mMinSelectableIndex = Integer.MIN_VALUE;
	private int mMaxSelectableIndex = Integer.MAX_VALUE;

	public WheelView(Context context) {
		super(context);
		init(null);
	}

	public WheelView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}

	public WheelView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(attrs);
	}

	protected void init(AttributeSet attrs) {
		float density = getResources().getDisplayMetrics().density;
		mMarkHighColor = 0xFFF74C39;
		mMarkNormColor = 0xFF666666;
		mMarkColor = 0xFFEEEEEE;
		mCursorSize = density * 18;
		mCenterTextSize = density * 32; // 22;
		mNormalTextSize = density * 48; // 18;
		mBottomSpace = density * 64;	// 6
		Log.w("mBottomSpace","mBottomSpace="+mBottomSpace);

		TypedArray ta = attrs == null ? null : getContext().obtainStyledAttributes(attrs, R.styleable.lwvWheelView);
		if (ta != null) {
			mMarkHighColor = ta.getColor(R.styleable.lwvWheelView_lwvHighlightColor, mMarkHighColor);
			mMarkNormColor = ta.getColor(R.styleable.lwvWheelView_lwvMarkTextColor, mMarkNormColor);
			mMarkColor = ta.getColor(R.styleable.lwvWheelView_lwvMarkColor, mMarkColor);
			mIntervalFactor = ta.getFloat(R.styleable.lwvWheelView_lwvIntervalFactor, mIntervalFactor);
			mMarkRatio = ta.getFloat(R.styleable.lwvWheelView_lwvMarkRatio, mMarkRatio);
			mCenterTextSize = ta.getDimension(R.styleable.lwvWheelView_lwvCenterMarkTextSize, mCenterTextSize);
			mNormalTextSize = ta.getDimension(R.styleable.lwvWheelView_lwvMarkTextSize, mNormalTextSize);
			mCursorSize = ta.getDimension(R.styleable.lwvWheelView_lwvCursorSize, mCursorSize);
		}
//		Log.w("mFadeMarkColor", String.format("#%06X", (0xFFFFFF & mFadeMarkColor)));
		mIntervalFactor = Math.max(1, mIntervalFactor);
		mMarkRatio = Math.min(1, mMarkRatio);
		mTopSpace = mCursorSize; // mCursorSize + density * 2;

		mMarkHighPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
		mMarkHighPaint.setTextAlign(Paint.Align.CENTER);
		mMarkHighPaint.setColor(mMarkHighColor);
		mMarkHighPaint.setTextSize(mCenterTextSize);

		mMarkNormPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
		mMarkNormPaint.setTextAlign(Paint.Align.CENTER);
		mMarkNormPaint.setColor(mMarkNormColor);
		mMarkNormPaint.setTextSize(mNormalTextSize);

		mOutlinePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
		mOutlinePaint.setTextAlign(Paint.Align.CENTER);
		mOutlinePaint.setColor(getResources().getColor(R.color.wheelOutline));
		mOutlinePaint.setTextSize(mCenterTextSize);
		mOutlinePaint.setStrokeWidth(8);
		mOutlinePaint.setStyle(Paint.Style.FILL_AND_STROKE);

		calcIntervalDis();

		mScroller = new OverScroller(getContext());
		mContentRectF = new RectF();
		selectIndex(0);
	}

	/**
	 * calculate interval distance between items
	 */
	private void calcIntervalDis() {
		if (mMarkHighPaint == null) {
			return;
		}
		String defaultText = "888888";
		Rect temp = new Rect();
		int max = 0;
		if (mItems != null && mItems.size() > 0) {
			for (String i : mItems) {
				mMarkHighPaint.getTextBounds(i, 0, i.length(), temp);
				if (temp.width() > max) {
					max = temp.width();
				}
			}
		} else {
			mMarkHighPaint.getTextBounds(defaultText, 0, defaultText.length(), temp);
			max = temp.width();
		}

		mIntervalDis = max * mIntervalFactor * 3 / 5;	// squeeze width
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
	}

	private int measureWidth(int widthMeasureSpec) {
		int measureMode = MeasureSpec.getMode(widthMeasureSpec);
		int measureSize = MeasureSpec.getSize(widthMeasureSpec);
		int result = getSuggestedMinimumWidth();
		switch (measureMode) {
			case MeasureSpec.AT_MOST:
			case MeasureSpec.EXACTLY:
				result = measureSize;
				break;
			default:
				break;
		}
		return result;
	}

	private int measureHeight(int heightMeasure) {
		int measureMode = MeasureSpec.getMode(heightMeasure);
		int measureSize = MeasureSpec.getSize(heightMeasure);
		int result = (int) (mBottomSpace + mTopSpace * 2 + mCenterTextSize);
		switch (measureMode) {
			case MeasureSpec.EXACTLY:
				result = Math.max(result, measureSize);
				break;
			case MeasureSpec.AT_MOST:
				result = Math.min(result, measureSize);
				break;
			default:
				break;
		}
		return result;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if (w != oldw || h != oldh) {
			mHeight = h;
			mMaxOverScrollDistance = w / 2.f;
			mContentRectF.set(0, 0, (mMarkCount - 1) * mIntervalDis, h);
			mViewScopeSize = (int) Math.ceil(mMaxOverScrollDistance / mIntervalDis);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		int start = mCenterIndex - mViewScopeSize;
		int end = mCenterIndex + mViewScopeSize + 1;

		start = Math.max(start, -mViewScopeSize * 2);
		end = Math.min(end, mMarkCount + mViewScopeSize * 2);

		// extends both ends
		if (mCenterIndex == mMaxSelectableIndex) {
			end += mViewScopeSize;
		} else if (mCenterIndex == mMinSelectableIndex) {
			start -= mViewScopeSize;
		}

		float x = start * mIntervalDis;

		for (int i = start; i < end; i++) {

			if (mMarkCount > 0 && i >= 0 && i < mMarkCount) {
				String temp = mItems.get(i);
				canvas.drawText(temp,  x, mHeight - 20- mBottomSpace, mOutlinePaint);
				if (mCenterIndex == i) {
					canvas.drawText(temp, x, mHeight - 20 - mBottomSpace, mMarkHighPaint);
				} else
					canvas.drawText(temp, x, mHeight - 20 - mBottomSpace, mMarkNormPaint);
			}
			x += mIntervalDis;
		}
	}

	@Override
	public void computeScroll() {
		super.computeScroll();
		if (mScroller.computeScrollOffset()) {
			scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
			refreshCenter();
			invalidate();
		} else {
			if (mFling) {
				mFling = false;
				autoSettle();
			}
		}
	}

	private void autoSettle() {
		int sx = getScrollX();
		float dx = mCenterIndex * mIntervalDis - sx - mMaxOverScrollDistance;
		mScroller.startScroll(sx, 0, (int) dx, 0);
		postInvalidate();
		if (mLastSelectedIndex != mCenterIndex) {
			mLastSelectedIndex = mCenterIndex;
		}
	}

	private int safeCenter(int center) {
		if (center < mMinSelectableIndex) {
			center = mMinSelectableIndex;
		} else if (center > mMaxSelectableIndex) {
			center = mMaxSelectableIndex;
		}
		return center;
	}

	private void refreshCenter(int offsetX) {
		int offset = (int) (offsetX + mMaxOverScrollDistance);
		int tempIndex = Math.round(offset / mIntervalDis);
		tempIndex = safeCenter(tempIndex);
		if (mCenterIndex == tempIndex) {
			return;
		}
		mCenterIndex = tempIndex;
	}

	private void refreshCenter() {
		refreshCenter(getScrollX());
	}

	public void selectIndex(int index) {
		mCenterIndex = index;
		post(new Runnable() {
			@Override
			public void run() {
				scrollTo((int) (mCenterIndex * mIntervalDis - mMaxOverScrollDistance), 0);
				invalidate();
				refreshCenter();
			}
		});
	}

	public void setItems(List<String> items) {
		if (mItems == null) {
			mItems = new ArrayList<>();
		} else {
			mItems.clear();
		}
		mItems.addAll(items);
		mMarkCount = null == mItems ? 0 : mItems.size();
		if (mMarkCount > 0) {
			mMinSelectableIndex = Math.max(mMinSelectableIndex, 0);
			mMaxSelectableIndex = Math.min(mMaxSelectableIndex, mMarkCount - 1);
		}
		mContentRectF.set(0, 0, (mMarkCount - 1) * mIntervalDis, getMeasuredHeight());
		mCenterIndex = Math.min(mCenterIndex, mMarkCount);
		calcIntervalDis();
		invalidate();
	}

	public int getSelectedPosition() {
		return mCenterIndex;
	}

	@Override
	public Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		SavedState ss = new SavedState(superState);
		ss.index = getSelectedPosition();
		ss.min = mMinSelectableIndex;
		ss.max = mMaxSelectableIndex;
		return ss;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		SavedState ss = (SavedState) state;
		super.onRestoreInstanceState(ss.getSuperState());
		mMinSelectableIndex = ss.min;
		mMaxSelectableIndex = ss.max;
		selectIndex(ss.index);
		requestLayout();
	}

	public interface OnWheelItemSelectedListener {
		void onWheelItemChanged(com.urrecliner.blackbox.WheelView wheelView, int position);

		void onWheelItemSelected(com.urrecliner.blackbox.WheelView wheelView, int position);
	}

	static class SavedState extends BaseSavedState {
		public static final Creator<SavedState> CREATOR
				= new Creator<SavedState>() {
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
		int index;
		int min;
		int max;

		SavedState(Parcelable superState) {
			super(superState);
		}

		private SavedState(Parcel in) {
			super(in);
			index = in.readInt();
			min = in.readInt();
			max = in.readInt();
		}
	}
}
