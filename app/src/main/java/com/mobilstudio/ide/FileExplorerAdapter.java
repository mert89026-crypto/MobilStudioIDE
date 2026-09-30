package com.mobilstudio.ide;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class FileExplorerAdapter extends BaseAdapter {

    private final Context context;
    private final ArrayList<ExplorerItem> items;

    public FileExplorerAdapter(Context context, ArrayList<ExplorerItem> items) {
        this.context = context;
        this.items = items;
    }

    @Override
    public int getCount() {
        return items == null ? 0 : items.size();
    }

    @Override
    public Object getItem(int position) {
        if (items == null || position < 0 || position >= items.size()) {
            return null;
        }

        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {

        ImageView icon;
        TextView name;
        TextView type;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        if (convertView == null) {

            convertView = createItemView(parent);

            holder = new ViewHolder();

            holder.icon = convertView.findViewWithTag("file_icon");
            holder.name = convertView.findViewWithTag("file_name");
            holder.type = convertView.findViewWithTag("file_type");

            convertView.setTag(holder);

        } else {

            holder = (ViewHolder) convertView.getTag();

        }

        ExplorerItem item = items.get(position);

        if (item == null) {
            holder.name.setText("");
            holder.type.setText("");
            holder.icon.setImageResource(android.R.drawable.ic_menu_help);
            return convertView;
        }

        holder.name.setText(item.getName());
        holder.type.setText(item.getDisplayType());

        // İkon
        holder.icon.setImageResource(getIconResource(item));

        // Klasörleri daha belirgin göster
        if (item.isFolder()) {

            holder.name.setTypeface(
                    Typeface.DEFAULT,
                    Typeface.BOLD
            );

            holder.name.setTextColor(
                    Color.parseColor("#111827")
            );

            holder.type.setTextColor(
                    Color.parseColor("#6B7280")
            );

        } else {

            holder.name.setTypeface(
                    Typeface.DEFAULT,
                    Typeface.NORMAL
            );

            holder.name.setTextColor(
                    Color.parseColor("#1F2937")
            );

            holder.type.setTextColor(
                    Color.parseColor("#9CA3AF")
            );
        }

        return convertView;
    }

    private int getIconResource(ExplorerItem item) {

        if (item.isFolder()) {

            return android.R.drawable.ic_menu_agenda;
        }

        if (item.isJavaFile()) {

            return android.R.drawable.ic_menu_edit;
        }

        if (item.isKotlinFile()) {

            return android.R.drawable.ic_menu_edit;
        }

        if (item.isXmlFile()) {

            return android.R.drawable.ic_menu_view;
        }

        if (item.isGradleFile()) {

            return android.R.drawable.ic_menu_manage;
        }

        if (item.isJsonFile()) {

            return android.R.drawable.ic_menu_edit;
        }

        String extension = item.getExtension();

        if (extension.equals("png")
                || extension.equals("jpg")
                || extension.equals("jpeg")
                || extension.equals("webp")
                || extension.equals("gif")) {

            return android.R.drawable.ic_menu_gallery;
        }

        if (extension.equals("jar")
                || extension.equals("aar")) {

            return android.R.drawable.ic_menu_save;
        }

        if (item.isTextFile()) {

            return android.R.drawable.ic_menu_edit;
        }

        return android.R.drawable.ic_menu_save;
    }

    private View createItemView(ViewGroup parent) {

        int paddingHorizontal = dp(12);
        int paddingVertical = dp(8);

        android.widget.LinearLayout root =
                new android.widget.LinearLayout(context);

        root.setOrientation(
                android.widget.LinearLayout.HORIZONTAL
        );

        root.setGravity(
                android.view.Gravity.CENTER_VERTICAL
        );

        root.setPadding(
                paddingHorizontal,
                paddingVertical,
                paddingHorizontal,
                paddingVertical
        );

        root.setMinimumHeight(dp(58));

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                Color.WHITE
        );

        root.setBackground(background);

        // İkon
        ImageView icon =
                new ImageView(context);

        icon.setTag("file_icon");

        icon.setLayoutParams(
                new android.widget.LinearLayout.LayoutParams(
                        dp(42),
                        dp(42)
                )
        );

        icon.setPadding(
                dp(7),
                dp(7),
                dp(7),
                dp(7)
        );

        root.addView(icon);

        // Sağ taraftaki yazılar
        android.widget.LinearLayout textContainer =
                new android.widget.LinearLayout(context);

        textContainer.setOrientation(
                android.widget.LinearLayout.VERTICAL
        );

        textContainer.setGravity(
                android.view.Gravity.CENTER_VERTICAL
        );

        android.widget.LinearLayout.LayoutParams
                textParams =
                new android.widget.LinearLayout.LayoutParams(
                        0,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        textParams.setMargins(
                dp(10),
                0,
                0,
                0
        );

        textContainer.setLayoutParams(textParams);

        // Dosya / klasör adı
        TextView name =
                new TextView(context);

        name.setTag("file_name");

        name.setTextSize(
                16
        );

        name.setSingleLine(
                true
        );

        name.setEllipsize(
                android.text.TextUtils.TruncateAt.END
        );

        name.setTextColor(
                Color.parseColor("#111827")
        );

        // Tür
        TextView type =
                new TextView(context);

        type.setTag("file_type");

        type.setTextSize(
                11
        );

        type.setSingleLine(
                true
        );

        type.setTextColor(
                Color.parseColor("#9CA3AF")
        );

        textContainer.addView(name);

        textContainer.addView(type);

        root.addView(textContainer);

        return root;
    }

    private int dp(int value) {

        float density =
                context.getResources()
                        .getDisplayMetrics()
                        .density;

        return (int) (
                value * density + 0.5f
        );
    }
}
