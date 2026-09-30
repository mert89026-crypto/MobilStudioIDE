package com.mobilstudio.ide;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

public class FileExplorerAdapter extends BaseAdapter {

    private final Context context;
    private final ArrayList<ExplorerItem> items;

    public FileExplorerAdapter(
            Context context,
            ArrayList<ExplorerItem> items
    ) {
        this.context = context;
        this.items = items;
    }

    @Override
    public int getCount() {

        if (items == null) {
            return 0;
        }

        return items.size();
    }

    @Override
    public Object getItem(int position) {

        if (items == null
                || position < 0
                || position >= items.size()) {
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
        TextView info;
        ImageButton menu;
    }

    @Override
    public View getView(
            int position,
            View convertView,
            ViewGroup parent
    ) {

        ViewHolder holder;

        if (convertView == null) {

            convertView = LayoutInflater.from(context)
                    .inflate(
                            R.layout.activity_file_item,
                            parent,
                            false
                    );

            holder = new ViewHolder();

            holder.icon = convertView.findViewById(
                    R.id.imgIcon
            );

            holder.name = convertView.findViewById(
                    R.id.txtName
            );

            holder.info = convertView.findViewById(
                    R.id.txtInfo
            );

            holder.menu = convertView.findViewById(
                    R.id.btnMenu
            );

            convertView.setTag(holder);

        } else {

            holder = (ViewHolder) convertView.getTag();
        }

        ExplorerItem item = items.get(position);

        if (item == null) {

            holder.name.setText("");
            holder.info.setText("");

            holder.icon.setImageResource(
                    android.R.drawable.ic_menu_help
            );

            holder.menu.setVisibility(View.GONE);

            return convertView;
        }

        /*
         * DOSYA / KLASÖR ADI
         */

        holder.name.setText(
                item.getName()
        );

        /*
         * DOSYA TÜRÜ
         */

        holder.info.setText(
                item.getDisplayType()
        );

        /*
         * İKON
         */

        holder.icon.setImageResource(
                getIconResource(item)
        );

        /*
         * KLASÖR GÖRÜNÜMÜ
         */

        if (item.isFolder()) {

            holder.name.setTextColor(
                    Color.parseColor("#111827")
            );

            holder.info.setTextColor(
                    Color.parseColor("#6B7280")
            );

            holder.name.setTypeface(
                    android.graphics.Typeface.DEFAULT,
                    android.graphics.Typeface.BOLD
            );

            holder.icon.setColorFilter(
                    Color.parseColor("#2563EB")
            );

        } else {

            holder.name.setTextColor(
                    Color.parseColor("#1F2937")
            );

            holder.info.setTextColor(
                    Color.parseColor("#9CA3AF")
            );

            holder.name.setTypeface(
                    android.graphics.Typeface.DEFAULT,
                    android.graphics.Typeface.NORMAL
            );

            holder.icon.setColorFilter(
                    Color.parseColor("#6B7280")
            );
        }

        /*
         * ÜÇ NOKTA MENÜSÜ
         *
         * Şimdilik sadece görünür durumda.
         * Sonraki aşamada:
         *
         * - Yeniden adlandır
         * - Sil
         * - Kopyala
         * - Taşı
         * - Paylaş
         *
         * gibi işlemleri buraya bağlayabiliriz.
         */

        holder.menu.setVisibility(View.VISIBLE);

        holder.menu.setOnClickListener(v -> {

            android.widget.PopupMenu popupMenu =
                    new android.widget.PopupMenu(
                            context,
                            holder.menu
                    );

            if (item.isFolder()) {

                popupMenu.getMenu().add(
                        "Klasörü aç"
                );

                popupMenu.getMenu().add(
                        "Yeniden adlandır"
                );

                popupMenu.getMenu().add(
                        "Sil"
                );

            } else {

                popupMenu.getMenu().add(
                        "Aç"
                );

                popupMenu.getMenu().add(
                        "Yeniden adlandır"
                );

                popupMenu.getMenu().add(
                        "Sil"
                );

                popupMenu.getMenu().add(
                        "Kopyala"
                );
            }

            popupMenu.setOnMenuItemClickListener(
                    menuItem -> {

                        String action =
                                menuItem.getTitle()
                                        .toString();

                        android.widget.Toast.makeText(
                                context,
                                action
                                        + ": "
                                        + item.getName(),
                                android.widget.Toast.LENGTH_SHORT
                        ).show();

                        return true;
                    }
            );

            popupMenu.show();
        });

        return convertView;
    }

    private int getIconResource(
            ExplorerItem item
    ) {

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

        String extension =
                item.getExtension()
                        .toLowerCase(Locale.ROOT);

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
}
