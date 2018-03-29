package liup.code.learnandroid.accessibility;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import liup.code.learnandroid.R;

/**
 * @title  演示如何实现自定义视图的可访问性支持。
 *          自定义视图是通过扩展android.view包中的基类开发的定制小部件。
 *          此示例显示如何通过继承（非向后兼容）和组合（向后兼容）实现可访问性行为。
 *
 *          虽然Android框架针对各种用例量身定制了不同的视图组合，但有时候开发人员需要的功能不是由标准视图实现的。
 *          一种解决方案是编写一个扩展基本视图类的自定义视图。
 *          在实现所需的功能时，开发人员还应该为该新功能实现可访问性支持，以便被禁用的用户可以利用它。
 *
 * @desc .
 * Created by liupeng on 2018/3/29.
 */
public class CustomViewAccessibilityActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_view_accessibility);
    }

    /**
     * 演示如何通过继承增强可访问性支持。
     *
     *  Note:   使用继承可能会破坏应用程序的向后兼容性。
     *          特别是，重写一个作为参数的方法或返回不存在于旧版本平台上的类将会阻止您的应用程序在该平台上运行。
     */
    public static class AccessibleCompoundButtonInheritance extends BaseToggleButton {

        public AccessibleCompoundButtonInheritance(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(event);
            // 我们调用超级实现来让超类设置适当的事件属性。 然后我们添加一个超级类不支持的新属性（checked）。
            event.setChecked(isChecked());
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);

            // 我们称超级实现为超类设置适当的信息属性。 然后我们添加我们的属性（可点击和点击），这些属性不受超类支持。
            info.setCheckable(true);
            info.setChecked(isChecked());

            // 很多时候，您只需要在自定义视图中添加文本。
            CharSequence text = getText();
            if (!TextUtils.isEmpty(text)) {
                info.setText(text);
            }
        }

        @Override
        public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
            super.onPopulateAccessibilityEvent(event);
            // 我们称超级实现为它的文本填充事件。
            // 然后我们添加我们的文本不在超级类中。
            // 很多时候，您只需要在自定义视图中添加文本。
            CharSequence text = getText();
            if (!TextUtils.isEmpty(text)) {
                event.getText().add(text);
            }
        }
    }

    /**
     *  演示如何通过组合增强可访问性支持。
     *
     *  Note:  使用组合可确保您的应用程序向后兼容。 android-support-v4库的API允许以向后兼容的方式使用可访问性API。
     *
     */
    public static class AccessibleCompoundButtonComposition extends BaseToggleButton {

        public AccessibleCompoundButtonComposition(Context context, AttributeSet attrs) {
            super(context, attrs);
            tryInstallAccessibilityDelegate();
        }

        public void tryInstallAccessibilityDelegate() {
            // 如果我们运行的平台的API版本太旧，并且不支持AccessibilityDelegate API，
            // 则不要调用View.setAccessibilityDelegate（AccessibilityDelegate）
            // 或引用AccessibilityDelegate，否则将引发异常。
            // 注意：android-support-v4库包含API，以向后兼容的方式使用可访问性API。
            if (Build.VERSION.SDK_INT < 14) {
                return;
            }
            // AccessibilityDelegate允许客户覆盖与View中的可访问性方法相对应的方法，并在View中注册委托，从而实质性地注入可访问性支持。
            setAccessibilityDelegate(new AccessibilityDelegate() {
                @Override
                public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
                    super.onInitializeAccessibilityEvent(host, event);
                    // 我们称超级实现为超类设置适当的事件属性。 然后我们添加一个超级类不支持的新属性（checked）。
                    event.setChecked(isChecked());
                }

                @Override
                public void onInitializeAccessibilityNodeInfo(View host,
                                                              AccessibilityNodeInfo info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    // 我们称超级实现为超类设置适当的信息属性。 然后我们添加我们的属性（checkable和checked），这些属性不受超类支持。
                    info.setCheckable(true);
                    info.setChecked(isChecked());
                    // 很多时候，您只需要在自定义视图中添加文本。
                    CharSequence text = getText();
                    if (!TextUtils.isEmpty(text)) {
                        info.setText(text);
                    }
                }

                @Override
                public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
                    super.onPopulateAccessibilityEvent(host, event);
                    // 我们称超级实现为它的文本填充事件。
                    // 然后我们添加我们的文本不在超级类中。
                    // 很多时候，您只需要在自定义视图中添加文本。
                    CharSequence text = getText();
                    if (!TextUtils.isEmpty(text)) {
                        event.getText().add(text);
                    }
                }
            });
        }
    }

    /**
     *  这是一个基本的切换按钮类，其可访问性不适合反映它实现的新功能。
     *
     *  Note:  这不是切换按钮的示例实现，而是需要演示如何优化自定义视图的辅助功能支持的简单类。
     *
     */
    private static class BaseToggleButton extends View {
        private boolean mChecked;

        private CharSequence mTextOn;
        private CharSequence mTextOff;

        private Layout mOnLayout;
        private Layout mOffLayout;

        private TextPaint mTextPaint;

        public BaseToggleButton(Context context, AttributeSet attrs) {
            this(context, attrs, android.R.attr.buttonStyle);
        }

        public BaseToggleButton(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);

            mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.textSize, typedValue, true);
            final int textSize = (int) typedValue.getDimension(
                    context.getResources().getDisplayMetrics());
            mTextPaint.setTextSize(textSize);

            context.getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
            final int textColor = context.getResources().getColor(typedValue.resourceId);
            mTextPaint.setColor(textColor);

            mTextOn = context.getString(R.string.accessibility_custom_on);
            mTextOff = context.getString(R.string.accessibility_custom_off);
        }

        public boolean isChecked() {
            return mChecked;
        }

        public CharSequence getText() {
            return mChecked ? mTextOn : mTextOff;
        }

        @Override
        public boolean performClick() {
            final boolean handled = super.performClick();
            if (!handled) {
                mChecked ^= true;
                invalidate();
            }
            return handled;
        }

        @Override
        public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            if (mOnLayout == null) {
                mOnLayout = makeLayout(mTextOn);
            }
            if (mOffLayout == null) {
                mOffLayout = makeLayout(mTextOff);
            }
            final int minWidth = Math.max(mOnLayout.getWidth(), mOffLayout.getWidth())
                    + getPaddingLeft() + getPaddingRight();
            final int minHeight = Math.max(mOnLayout.getHeight(), mOffLayout.getHeight())
                    + getPaddingLeft() + getPaddingRight();
            setMeasuredDimension(resolveSizeAndState(minWidth, widthMeasureSpec, 0),
                    resolveSizeAndState(minHeight, heightMeasureSpec, 0));
        }

        private Layout makeLayout(CharSequence text) {
            return new StaticLayout(text, mTextPaint,
                    (int) Math.ceil(Layout.getDesiredWidth(text, mTextPaint)),
                    Layout.Alignment.ALIGN_NORMAL, 1.f, 0, true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.save();
            canvas.translate(getPaddingLeft(), getPaddingRight());
            Layout switchText = mChecked ? mOnLayout : mOffLayout;
            switchText.draw(canvas);
            canvas.restore();
        }
    }
}
