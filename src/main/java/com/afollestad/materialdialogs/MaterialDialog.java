package com.afollestad.materialdialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.support.annotation.ArrayRes;
import android.support.annotation.ColorRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.text.method.LinkMovementMethod;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * @author Aidan Follestad (afollestad)
 */
public class MaterialDialog extends AlertDialog implements View.OnClickListener {

    private final static String POSITIVE = "POSITIVE";
    private final static String NEGATIVE = "NEGATIVE";
    private final static String NEUTRAL = "NEUTRAL";

    private Context mContext;
    private String positiveText;
    private TextView positiveButton;
    private String neutralText;
    private TextView neutralButton;
    private String negativeText;
    private TextView negativeButton;
    private View view;
    private Theme theme;
    private int positiveColor;
    private SimpleCallback callback;
    private ListCallback listCallback;
    private ListCallbackMulti listCallbackMulti;
    private View customView;
    private float buttonHeight;
    private String[] items;
    private boolean isStacked;

    MaterialDialog(Builder builder) {
        super(new ContextThemeWrapper(builder.context, builder.theme == Theme.LIGHT ? R.style.Light : R.style.Dark));
        mContext = builder.context;
        view = LayoutInflater.from(builder.context).inflate(R.layout.material_dialog, null);
        customView = builder.customView;

        TextView title = (TextView) view.findViewById(R.id.title);
        TextView body = (TextView) view.findViewById(R.id.content);

        body.setText(builder.content);
        body.setMovementMethod(new LinkMovementMethod());
        body.setVisibility(View.VISIBLE);
        if (builder.theme == Theme.LIGHT) {
            body.setTextColor(LightColors.CONTENT.get());
        } else {
            body.setTextColor(DarkColors.CONTENT.get());
        }

        this.callback = builder.callback;
        this.listCallback = builder.listCallback;
        this.listCallbackMulti = builder.listCallbackMulti;
        this.positiveText = builder.positiveText;
        this.neutralText = builder.neutralText;
        this.negativeText = builder.negativeText;
        this.theme = builder.theme;
        this.positiveColor = builder.positiveColor;
        this.items = builder.items;

        if (customView != null) {
            title = (TextView) view.findViewById(R.id.titleCustomView);
            buttonHeight = mContext.getResources().getDimension(R.dimen.button_height_customview);
            view.findViewById(R.id.mainFrame).setVisibility(View.GONE);
            view.findViewById(R.id.customViewScroll).setVisibility(View.VISIBLE);
            view.findViewById(R.id.customViewDivider).setVisibility(View.VISIBLE);
            ((LinearLayout) view.findViewById(R.id.customViewFrame)).addView(customView);
        } else {
            buttonHeight = mContext.getResources().getDimension(R.dimen.button_height);
            view.findViewById(R.id.mainFrame).setVisibility(View.VISIBLE);
            view.findViewById(R.id.customViewScroll).setVisibility(View.GONE);
            view.findViewById(R.id.customViewDivider).setVisibility(View.GONE);
        }

        // Title is set after it's determined whether to use first title or custom view title
        title.setText(builder.title);
        if (builder.theme == Theme.LIGHT) {
            title.setTextColor(LightColors.TITLE.get());
        } else {
            title.setTextColor(DarkColors.TITLE.get());
        }
        if (builder.titleAlignment == Alignment.CENTER) {
            title.setGravity(Gravity.CENTER_HORIZONTAL);
        } else if (builder.titleAlignment == Alignment.RIGHT) {
            title.setGravity(Gravity.RIGHT);
        }

        invalidateList();
        invalidateActions();
        checkIfStackingNeeded();
        setView(view);
    }

    private void invalidateList() {
        if (items == null || items.length == 0) return;
        view.findViewById(R.id.content).setVisibility(View.GONE);
        View mainFrame = view.findViewById(R.id.mainFrame);
        mainFrame.setPadding(mainFrame.getPaddingLeft(), mainFrame.getPaddingTop(), mainFrame.getPaddingRight(), 0);

        View title = view.findViewById(R.id.title);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) title.getLayoutParams();
        params.bottomMargin = (int) mContext.getResources().getDimension(R.dimen.title_margin_customview);
        title.setLayoutParams(params);
        int dp = (int) mContext.getResources().getDimension(R.dimen.button_padding);
        title.setPadding(dp, 0, dp, 0);

        LinearLayout list = (LinearLayout) view.findViewById(R.id.listFrame);
        list.setVisibility(View.VISIBLE);
        LayoutInflater li = LayoutInflater.from(mContext);

        for (int index = 0; index < items.length; index++) {
            View il;
            if (listCallbackMulti == null) {
                il = li.inflate(R.layout.dialog_listitem, null);
                ((TextView) il).setText(items[index]);
                if (this.theme == Theme.LIGHT) {
                    ((TextView) il).setTextColor(LightColors.ITEM.get());
                } else {
                    ((TextView) il).setTextColor(DarkColors.ITEM.get());
                }
            } else {
                il = li.inflate(R.layout.dialog_listitem_multichoice, null);
                CheckBox cb = (CheckBox) ((LinearLayout) il).getChildAt(0);
                cb.setText(items[index]);
                if (this.theme == Theme.LIGHT) {
                    cb.setTextColor(LightColors.ITEM.get());
                } else {
                    cb.setTextColor(DarkColors.ITEM.get());
                }
            }
            il.setTag(index + ":" + items[index]);
            il.setOnClickListener(this);
            list.addView(il);
        }
    }

    private void checkIfStackingNeeded() {
        if ((negativeButton.getVisibility() == View.GONE && neutralButton.getVisibility() == View.GONE)) {
            // Stacking isn't necessary if you only have one button
            return;
        }
        Paint paint = positiveButton.getPaint();
        float px = mContext.getResources().getDimension(R.dimen.button_regular_width);
        isStacked = paint.measureText(positiveButton.getText().toString()) > px;
        if (this.neutralText != null)
            isStacked = isStacked || paint.measureText(neutralButton.getText().toString()) > px;
        if (this.negativeText != null)
            isStacked = isStacked || paint.measureText(negativeButton.getText().toString()) > px;
        invalidateActions();
    }

    private void invalidateHeightAndMargin(View button, boolean bottom) {
        if (button.getLayoutParams() instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) button.getLayoutParams();
            params.height = (int) buttonHeight;
            if (customView != null) {
                params.bottomMargin = 0;
            } else {
                if (isStacked && !bottom) return;
                params.bottomMargin = (int) mContext.getResources().getDimension(R.dimen.button_frame_margin);
            }
            button.setLayoutParams(params);
        } else {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) button.getLayoutParams();
            params.height = (int) buttonHeight;
            if (customView != null) {
                params.bottomMargin = 0;
            } else {
                if (isStacked && !bottom) return;
                params.bottomMargin = (int) mContext.getResources().getDimension(R.dimen.button_frame_margin);
            }
            button.setLayoutParams(params);
        }
    }

    private void invalidateActions() {
        if (items != null && listCallbackMulti == null) {
            // If the dialog is a plain list dialog, no buttons are shown.
            view.findViewById(R.id.buttonDefaultFrame).setVisibility(View.GONE);
            view.findViewById(R.id.buttonStackedFrame).setVisibility(View.GONE);
            return;
        }

        view.findViewById(R.id.buttonDefaultFrame).setVisibility(isStacked ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.buttonStackedFrame).setVisibility(isStacked ? View.VISIBLE : View.GONE);

        positiveButton = (TextView) view.findViewById(
                isStacked ? R.id.buttonStackedPositive : R.id.buttonDefaultPositive);
        if (this.positiveText == null)
            this.positiveText = mContext.getString(R.string.accept);
        positiveButton.setText(this.positiveText);
        positiveButton.setTextColor(this.positiveColor);
        invalidateHeightAndMargin(positiveButton, negativeText == null && neutralText == null);
        positiveButton.setTag(POSITIVE);
        positiveButton.setOnClickListener(this);

        neutralButton = (TextView) view.findViewById(
                isStacked ? R.id.buttonStackedNeutral : R.id.buttonDefaultNeutral);
        if (this.neutralText != null) {
            neutralButton.setVisibility(View.VISIBLE);
            if (this.theme == Theme.LIGHT) {
                neutralButton.setTextColor(LightColors.BUTTON.get());
            } else {
                neutralButton.setTextColor(DarkColors.BUTTON.get());
            }
            neutralButton.setText(this.neutralText);
            invalidateHeightAndMargin(neutralButton, true);
            neutralButton.setTag(NEUTRAL);
            neutralButton.setOnClickListener(this);
        } else {
            neutralButton.setVisibility(View.GONE);
        }

        negativeButton = (TextView) view.findViewById(
                isStacked ? R.id.buttonStackedNegative : R.id.buttonDefaultNegative);
        if (this.negativeText != null) {
            negativeButton.setVisibility(View.VISIBLE);
            if (this.theme == Theme.LIGHT) {
                negativeButton.setTextColor(LightColors.BUTTON.get());
            } else {
                negativeButton.setTextColor(DarkColors.BUTTON.get());
            }
            negativeButton.setText(this.negativeText);
            invalidateHeightAndMargin(negativeButton, neutralText == null);
            negativeButton.setTag(NEGATIVE);
            negativeButton.setOnClickListener(this);
        } else {
            negativeButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        String tag = (String) v.getTag();
        if (tag.equals(POSITIVE)) {
            if (callback != null) {
                dismiss();
                callback.onPositive();
            }
        } else if (tag.equals(NEGATIVE)) {
            if (callback != null && callback instanceof Callback) {
                dismiss();
                ((Callback) callback).onNegative();
            }
        } else if (tag.equals(NEUTRAL)) {
            if (callback != null && callback instanceof FullCallback) {
                dismiss();
                ((FullCallback) callback).onNeutral();
            }
        } else if (listCallback != null) {
            dismiss();
            String[] split = tag.split(":");
            int index = Integer.parseInt(split[0]);
            listCallback.onSelection(index, split[1]);
        } else if (listCallbackMulti != null) {
            CheckBox cb = (CheckBox) ((LinearLayout) v).getChildAt(0);
            cb.performClick();
        }
    }

    public View getCustomView() {
        return customView;
    }

    public static class Builder {

        protected Activity context;
        protected String title;
        protected String content;
        protected String[] items;
        protected String positiveText;
        protected String neutralText;
        protected String negativeText;
        protected View customView;
        protected int positiveColor;
        protected SimpleCallback callback;
        protected ListCallback listCallback;
        private ListCallbackMulti listCallbackMulti;
        protected Theme theme = Theme.LIGHT;
        protected Alignment titleAlignment = Alignment.LEFT;

        public Builder(@NonNull Activity context) {
            this.context = context;
            this.positiveText = context.getString(R.string.accept);
            final int materialBlue = context.getResources().getColor(R.color.material_blue_500);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{android.R.attr.colorAccent});
                try {
                    this.positiveColor = a.getColor(0, materialBlue);
                } finally {
                    a.recycle();
                }
            } else {
                this.positiveColor = materialBlue;
            }
        }

        public Builder title(@StringRes int titleRes) {
            title(this.context.getString(titleRes));
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder titleAlignment(Alignment align) {
            this.titleAlignment = align;
            return this;
        }

        public Builder content(@StringRes int contentRes) {
            content(this.context.getString(contentRes));
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder items(@ArrayRes int itemsRes) {
            items(this.context.getResources().getStringArray(itemsRes));
            return this;
        }

        public Builder items(String[] items) {
            this.items = items;
            return this;
        }

        public Builder itemsCallback(ListCallback callback) {
            this.listCallback = callback;
            this.listCallbackMulti = null;
            return this;
        }

        public Builder itemsCallbackMulti(ListCallbackMulti callback) {
            this.listCallback = null;
            this.listCallbackMulti = callback;
            return this;
        }

        public Builder positiveText(@StringRes int postiveRes) {
            positiveText(this.context.getString(postiveRes));
            return this;
        }

        public Builder positiveText(String message) {
            this.positiveText = message;
            return this;
        }

        public Builder neutralText(@StringRes int neutralRes) {
            neutralText(this.context.getString(neutralRes));
            return this;
        }

        public Builder neutralText(String message) {
            this.neutralText = message;
            return this;
        }

        public Builder negativeText(@StringRes int negativeRes) {
            negativeText(this.context.getString(negativeRes));
            return this;
        }

        public Builder negativeText(String message) {
            this.negativeText = message;
            return this;
        }

        public Builder customView(@LayoutRes int layoutRes) {
            LayoutInflater li = LayoutInflater.from(this.context);
            customView(li.inflate(layoutRes, null));
            return this;
        }

        public Builder customView(View view) {
            this.customView = view;
            return this;
        }

        public Builder positiveColorRes(@ColorRes int colorRes) {
            positiveColor(this.context.getResources().getColor(colorRes));
            return this;
        }

        public Builder positiveColor(int color) {
            this.positiveColor = color;
            return this;
        }

        public Builder callback(SimpleCallback callback) {
            this.callback = callback;
            return this;
        }

        public Builder theme(Theme theme) {
            this.theme = theme;
            return this;
        }

        public MaterialDialog build() {
            return new MaterialDialog(this);
        }
    }


    public static interface ListCallback {
        void onSelection(int which, String text);
    }

    public static interface ListCallbackMulti {
        void onSelection(int[] which, String[] text);
    }

    public static interface SimpleCallback {
        void onPositive();
    }

    public static interface Callback extends SimpleCallback {
        void onPositive();

        void onNegative();
    }

    public static interface FullCallback extends Callback {
        void onNeutral();
    }

    static enum LightColors {
        TITLE("#3C3C3D"), CONTENT("#535353"), ITEM("#535353"), BUTTON("#3C3C3D");

        final String mColor;

        LightColors(String color) {
            this.mColor = color;
        }

        public int get() {
            return Color.parseColor(mColor);
        }
    }

    static enum DarkColors {
        TITLE("#FFFFFF"), CONTENT("#EDEDED"), ITEM("#EDEDED"), BUTTON("#FFFFFF");

        final String mColor;

        DarkColors(String color) {
            this.mColor = color;
        }

        public int get() {
            return Color.parseColor(mColor);
        }
    }
}
