package android.iocl.dac_collector.Utility;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.iocl.dac_collector.R;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NewsDialog {

    public interface NewsDialogListener {
        void onPrimaryAction();
        void onSecondaryAction();
        void onDismiss();
    }

    public enum NewsType {
        INFO, WARNING, SUCCESS
    }

    private Dialog dialog;
    private NewsDialogListener listener;

    public static class Builder {
        private Activity activity;
        private String title;
        private String Toptitle;
        private String publishTime;
        private String content;
        private String primaryButtonText;
        private Boolean setBanglaLanguage;
        private String secondaryButtonText;
        private NewsType newsType = NewsType.INFO;
        private boolean showImage = false;
        private String imageUrl;
        private boolean cancelable = true;
        private NewsDialogListener listener;
        private boolean compact = false;

        public Builder(Activity activity) {
            this.activity = activity;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }
        public Builder setTopTitle(String TopTitle) {
            this.Toptitle = TopTitle;
            return this;
        }
        public Builder setPublishTime(String publishTime) {
            this.publishTime = publishTime;
            return this;
        }

        public Builder setContent(String content) {
            this.content = content;
            return this;
        }

        public Builder setPrimaryButtonText(String text) {
            this.primaryButtonText = text;
            return this;
        }
        public Builder setBanglaFont(Boolean isBangla) {
            this.setBanglaLanguage = isBangla;
            return this;
        }

        public Builder setSecondaryButtonText(String text) {
            this.secondaryButtonText = text;
            return this;
        }

        public Builder setNewsType(NewsType type) {
            this.newsType = type;
            return this;
        }

        public Builder showImage(boolean show) {
            this.showImage = show;
            return this;
        }

        public Builder setImageUrl(String url) {
            this.imageUrl = url;
            return this;
        }

        public Builder setCancelable(boolean cancelable) {
            this.cancelable = cancelable;
            return this;
        }

        public Builder setListener(NewsDialogListener listener) {
            this.listener = listener;
            return this;
        }

        public Builder setCompact(boolean compact) {
            this.compact = compact;
            return this;
        }

        public NewsDialog build() {
            return new NewsDialog(this);
        }

        public void show() {
            build().show();
        }
    }

    private NewsDialog(Builder builder) {
        initialize(builder);
    }

    private void initialize(Builder builder) {
        dialog = new Dialog(builder.activity);

        int layoutRes = /*builder.compact ? R.layout.dialog_news_compact :*/ R.layout.dialog_important_news;
        dialog.setContentView(layoutRes);
        dialog.setCancelable(builder.cancelable);
        dialog.setCanceledOnTouchOutside(builder.cancelable);

        this.listener = builder.listener;

        // Initialize views based on layout
        if (builder.compact) {
            setupCompactDialog(builder);
        } else {
            setupFullDialog(builder);
        }

        // Set dialog window properties
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            if (builder.compact) {
                window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            } else {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void setupFullDialog(Builder builder) {
        TextView tvTitle = dialog.findViewById(R.id.tvTitle);
        TextView tvDate = dialog.findViewById(R.id.tvDate);
        TextView tvNewsTitle = dialog.findViewById(R.id.tvNewsTitle);
        TextView tvNewsContent = dialog.findViewById(R.id.tvNewsContent);
        ImageView ivNewsIcon = dialog.findViewById(R.id.ivNewsIcon);
        ImageView ivNewsImage = dialog.findViewById(R.id.ivNewsImage);
        Button btnPrimary = dialog.findViewById(R.id.btnPrimary);
        Button btnSecondary = dialog.findViewById(R.id.btnSecondary);
        ImageButton btnClose = dialog.findViewById(R.id.btnClose);
        LinearLayout headerLayout = dialog.findViewById(R.id.headerLayout);

        // Set data
        tvNewsTitle.setText(builder.title);
        tvTitle.setText(builder.Toptitle);
        tvNewsContent.setText(builder.content);
        tvDate.setText(builder.publishTime.isEmpty() ? getCurrentDateTime() : builder.publishTime);

        if (builder.setBanglaLanguage){
            Typeface typeface = ResourcesCompat.getFont(builder.activity, R.font.li_sohid_unicode);
            tvNewsTitle.setTypeface(typeface);
            tvNewsContent.setTypeface(typeface);
        }

        // Set news type styling
        setupNewsTypeStyling(builder.newsType, headerLayout, ivNewsIcon);

        // Setup image
        if (builder.showImage && builder.imageUrl != null) {
            ivNewsImage.setVisibility(View.VISIBLE);
            // You can use Picasso or Glide here to load the image
            // Picasso.get().load(builder.imageUrl).into(ivNewsImage);
        }

        // Setup buttons
        if (builder.primaryButtonText != null) {
            btnPrimary.setText(builder.primaryButtonText);

        }

        btnPrimary.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPrimaryAction();
            }
            dialog.dismiss();
        });

        if (builder.secondaryButtonText != null) {
            btnSecondary.setVisibility(View.VISIBLE);
            btnSecondary.setText(builder.secondaryButtonText);
            btnSecondary.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSecondaryAction();
                }
                dialog.dismiss();
            });
        }

        btnClose.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDismiss();
            }
            dialog.dismiss();
        });

        // Handle outside touch
        if (!builder.cancelable) {
            btnClose.setVisibility(View.GONE);
        }
    }

    private void setupCompactDialog(Builder builder) {
        TextView tvNewsTitle = dialog.findViewById(R.id.tvNewsTitle);
        TextView tvNewsContent = dialog.findViewById(R.id.tvNewsContent);
        Button btnPrimary = dialog.findViewById(R.id.btnPrimary);
        ImageButton btnClose = dialog.findViewById(R.id.btnClose);
        LinearLayout headerLayout = dialog.findViewById(R.id.headerLayout);

        tvNewsTitle.setText(builder.title);
        tvNewsContent.setText(builder.content);

        // Style for compact version
        setupCompactNewsTypeStyling(builder.newsType, headerLayout);

        if (builder.primaryButtonText != null) {
            btnPrimary.setText(builder.primaryButtonText);
        }

        btnPrimary.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPrimaryAction();
            }
            dialog.dismiss();
        });

        btnClose.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDismiss();
            }
            dialog.dismiss();
        });
    }

    private void setupNewsTypeStyling(NewsType newsType, LinearLayout header, ImageView icon) {
        int headerBg = R.drawable.news_header_gradient;
        int iconRes = R.drawable.user_icon;

        switch (newsType) {
            case INFO:
                headerBg = R.drawable.info_header_gradient;
                iconRes = R.drawable.user_icon;
                break;
            case WARNING:
                headerBg = R.drawable.warning_header_gradient;
                iconRes = R.drawable.user_icon;
                break;
            case SUCCESS:
                headerBg = R.drawable.success_header_gradient;
                iconRes = R.drawable.user_icon;
                break;
            default:
                headerBg = R.drawable.success_header_gradient;
                iconRes = R.drawable.user_icon;
                break;

        }

        header.setBackgroundResource(headerBg);
        icon.setImageResource(iconRes);
    }

    private void setupCompactNewsTypeStyling(NewsType newsType, LinearLayout header) {
        int headerBg = R.drawable.news_banner_gradient;

        switch (newsType) {
            case INFO:
                headerBg = R.drawable.info_banner_gradient;
                break;
            case WARNING:
                headerBg = R.drawable.warning_banner_gradient;
                break;
            case SUCCESS:
                headerBg = R.drawable.success_banner_gradient;
                break;
        }

        header.setBackgroundResource(headerBg);
    }

    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
        return sdf.format(new Date());
    }

    public void show() {
        if (dialog != null && !dialog.isShowing()) {
            dialog.show();
        }
    }

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
