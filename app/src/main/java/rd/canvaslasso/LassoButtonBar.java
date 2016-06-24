package rd.canvaslasso;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Wei on 2016/6/23.
 */
public class LassoButtonBar extends RelativeLayout {
    @Bind(R.id.button_cancel)
    Button cancelButton;
    @Bind(R.id.button_cut)
    Button cutButton;
    @Bind(R.id.button_copy)
    Button copyButton;
    @Bind(R.id.button_paste)
    Button pasteButton;

    private OnClickListener onCancelButtonClickListener;
    private OnClickListener onCutButtonClickListener;
    private OnClickListener onCopyButtonClickListener;
    private OnClickListener onPasteButtonClickListener;

    public LassoButtonBar(Context context) {
        super(context);
        View view = LayoutInflater.from(context).inflate(R.layout.widget_lasso_button_bar, null);
        addView(view);
        ButterKnife.bind(this);
        pasteButton.setVisibility(GONE);
    }

    public void setOnCancelButtonClickListener(OnClickListener onCancelButtonClickListener) {
        this.onCancelButtonClickListener = onCancelButtonClickListener;
    }

    public void setOnCutButtonClickListener(OnClickListener onCutButtonClickListener) {
        this.onCutButtonClickListener = onCutButtonClickListener;
    }

    public void setOnCopyButtonClickListener(OnClickListener onCopyButtonClickListener) {
        this.onCopyButtonClickListener = onCopyButtonClickListener;
    }

    public void setOnPasteButtonClickListener(OnClickListener onPasteButtonClickListener) {
        this.onPasteButtonClickListener = onPasteButtonClickListener;
    }

    @OnClick(R.id.button_cancel)
    public void onCancelButtonClick(View v) {
        if (onCancelButtonClickListener != null)
            onCancelButtonClickListener.onClick(v);
    }

    @OnClick(R.id.button_cut)
    public void onCutButtonClick(View v) {
        cutButton.setVisibility(GONE);
        copyButton.setVisibility(GONE);
        pasteButton.setVisibility(VISIBLE);

        if (onCutButtonClickListener != null)
            onCutButtonClickListener.onClick(v);
    }

    @OnClick(R.id.button_copy)
    public void onCopyButtonClick(View v) {
        cutButton.setVisibility(GONE);
        copyButton.setVisibility(GONE);
        pasteButton.setVisibility(VISIBLE);
        if (onCopyButtonClickListener != null)
            onCopyButtonClickListener.onClick(v);
    }

    @OnClick(R.id.button_paste)
    public void onPasteButtonClick(View v) {
        if (onPasteButtonClickListener != null)
            onPasteButtonClickListener.onClick(v);
    }
}
