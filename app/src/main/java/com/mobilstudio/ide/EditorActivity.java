package com.mobilstudio.ide;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

public class EditorActivity extends AppCompatActivity {

    private EditText etFilePath;
    private EditText etCode;

    private Button btnSave;
    private Button btnOpen;
    private Button btnBuild;

    private LinearLayout floatingPreview;
    private FrameLayout phoneCanvas;

    private TextView txtStatus;

    private float dX;
    private float dY;

    private File repoDir;
    private File currentExplorerDir;
    private File currentFile;

    private final Handler previewHandler =
            new Handler();

    private Runnable previewRunnable;

    /*
     * Dosya gezgini
     */
    private Dialog fileExplorerDialog;

    private LinearLayout explorerRoot;
    private LinearLayout explorerListContainer;

    private TextView explorerTitle;
    private TextView explorerPath;

    private EditText explorerSearch;

    private File explorerDirectory;

    private ArrayList<ExplorerItem> explorerItems =
            new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_editor
        );

        /*
         * ========================================================
         * PROJE YOLU
         * ========================================================
         */

        String receivedRepoPath =
                getIntent().getStringExtra(
                        "REPO_PATH"
                );

        if (receivedRepoPath == null
                || receivedRepoPath.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "Proje yolu alınamadı",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        repoDir =
                new File(
                        receivedRepoPath
                );

        if (!repoDir.exists()
                || !repoDir.isDirectory()) {

            Toast.makeText(
                    this,
                    "Proje klasörü bulunamadı",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        currentExplorerDir =
                repoDir;

        /*
         * ========================================================
         * VIEW'LAR
         * ========================================================
         */

        etFilePath =
                findViewById(
                        R.id.etFilePath
                );

        etCode =
                findViewById(
                        R.id.etCode
                );

        btnSave =
                findViewById(
                        R.id.btnSave
                );

        btnOpen =
                findViewById(
                        R.id.btnOpen
                );

        btnBuild =
                findViewById(
                        R.id.btnBuild
                );

        floatingPreview =
                findViewById(
                        R.id.floatingPreview
                );

        phoneCanvas =
                findViewById(
                        R.id.phoneCanvas
                );

        txtStatus =
                findViewById(
                        R.id.txtStatus
                );

        /*
         * ========================================================
         * BAŞLANGIÇ
         * ========================================================
         */

        etFilePath.setText("");

        setStatus(
                "Proje: " + repoDir.getName()
        );

        setupFloatingPreview();

        /*
         * ========================================================
         * XML DEĞİŞİNCE ÖNİZLEME
         * ========================================================
         */

        etCode.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {

                        schedulePreview(
                                s.toString()
                        );
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }
                }
        );

        /*
         * ========================================================
         * KAYDET
         * ========================================================
         */

        btnSave.setOnClickListener(
                v -> saveCurrentFile()
        );

        /*
         * ========================================================
         * AÇ
         * ========================================================
         */

        btnOpen.setOnClickListener(
                v -> showFileExplorer(
                        repoDir
                )
        );

        /*
         * ========================================================
         * DOSYA YOLU
         * ========================================================
         */

        etFilePath.setOnClickListener(
                v -> showFileExplorer(
                        currentExplorerDir == null
                                ? repoDir
                                : currentExplorerDir
                )
        );

        /*
         * ========================================================
         * DERLE
         * ========================================================
         */

        btnBuild.setOnClickListener(
                v -> {

                    Toast.makeText(
                            this,
                            "APK derleme sistemi henüz bağlanmadı.",
                            Toast.LENGTH_SHORT
                    ).show();

                    setStatus(
                            "Derleme sistemi henüz bağlanmadı"
                    );
                }
        );

        phoneCanvas.removeAllViews();
    }

    /*
     * ============================================================
     * YÜZEN ÖNİZLEME
     * ============================================================
     */

    private void setupFloatingPreview() {

        floatingPreview.setOnTouchListener(
                (view, event) -> {

                    switch (event.getAction()) {

                        case MotionEvent.ACTION_DOWN:

                            dX =
                                    view.getX()
                                            - event.getRawX();

                            dY =
                                    view.getY()
                                            - event.getRawY();

                            return true;

                        case MotionEvent.ACTION_MOVE:

                            float newX =
                                    event.getRawX()
                                            + dX;

                            float newY =
                                    event.getRawY()
                                            + dY;

                            View parent =
                                    (View) view.getParent();

                            if (parent != null) {

                                int maxX =
                                        parent.getWidth()
                                                - view.getWidth();

                                int maxY =
                                        parent.getHeight()
                                                - view.getHeight();

                                newX =
                                        Math.max(
                                                0,
                                                Math.min(
                                                        newX,
                                                        maxX
                                                )
                                        );

                                newY =
                                        Math.max(
                                                0,
                                                Math.min(
                                                        newY,
                                                        maxY
                                                )
                                        );
                            }

                            view.setX(newX);
                            view.setY(newY);

                            return true;

                        case MotionEvent.ACTION_UP:

                            return true;
                    }

                    return true;
                }
        );
    }

    /*
     * ============================================================
     * ÖNİZLEME GECİKMESİ
     * ============================================================
     */

    private void schedulePreview(
            String xmlCode
    ) {

        if (previewRunnable != null) {

            previewHandler.removeCallbacks(
                    previewRunnable
            );
        }

        previewRunnable =
                () -> renderLiveXml(
                        xmlCode
                );

        previewHandler.postDelayed(
                previewRunnable,
                250
        );
    }

    /*
     * ============================================================
     * DOSYA GEZGİNİNİ AÇ
     * ============================================================
     */

    private void showFileExplorer(
            File directory
    ) {

        if (directory == null) {
            return;
        }

        if (!directory.exists()
                || !directory.isDirectory()) {

            Toast.makeText(
                    this,
                    "Klasör bulunamadı",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (!isInsideProject(directory)) {

            Toast.makeText(
                    this,
                    "Bu klasöre erişilemez",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        currentExplorerDir =
                directory;

        /*
         * Daha önce açık dialog varsa
         * sadece klasörü yenile.
         */

        if (fileExplorerDialog != null
                && fileExplorerDialog.isShowing()) {

            explorerDirectory =
                    directory;

            refreshExplorer();

            return;
        }

        explorerDirectory =
                directory;

        fileExplorerDialog =
                new Dialog(this);

        fileExplorerDialog.requestWindowFeature(
                Window.FEATURE_NO_TITLE
        );

        explorerRoot =
                new LinearLayout(this);

        explorerRoot.setOrientation(
                LinearLayout.VERTICAL
        );

        explorerRoot.setBackgroundColor(
                Color.rgb(
                        20,
                        23,
                        32
                )
        );

        /*
         * ========================================================
         * ÜST BAR
         * ========================================================
         */

        LinearLayout topBar =
                new LinearLayout(this);

        topBar.setOrientation(
                LinearLayout.HORIZONTAL
        );

        topBar.setGravity(
                Gravity.CENTER_VERTICAL
        );

        topBar.setPadding(
                dp(8),
                dp(6),
                dp(6),
                dp(6)
        );

        topBar.setBackgroundColor(
                Color.rgb(
                        48,
                        48,
                        80
                )
        );

        /*
         * GERİ
         */

        TextView back =
                createTopIcon(
                        "‹"
                );

        back.setOnClickListener(
                v -> {

                    if (explorerDirectory.equals(
                            repoDir
                    )) {

                        fileExplorerDialog.dismiss();

                        return;
                    }

                    File parent =
                            explorerDirectory
                                    .getParentFile();

                    if (parent != null
                            && isInsideProject(parent)) {

                        explorerDirectory =
                                parent;

                        refreshExplorer();
                    }
                }
        );

        topBar.addView(
                back,
                new LinearLayout.LayoutParams(
                        dp(48),
                        dp(52)
                )
        );

        /*
         * BAŞLIK + YOL
         */

        LinearLayout titleBox =
                new LinearLayout(this);

        titleBox.setOrientation(
                LinearLayout.VERTICAL
        );

        titleBox.setGravity(
                Gravity.CENTER_VERTICAL
        );

        explorerTitle =
                new TextView(this);

        explorerTitle.setTextColor(
                Color.WHITE
        );

        explorerTitle.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                21
        );

        explorerTitle.setTypeface(
                Typeface.DEFAULT,
                Typeface.NORMAL
        );

        explorerPath =
                new TextView(this);

        explorerPath.setTextColor(
                Color.WHITE
        );

        explorerPath.setAlpha(
                0.75f
        );

        explorerPath.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                12
        );

        titleBox.addView(
                explorerTitle
        );

        titleBox.addView(
                explorerPath
        );

        topBar.addView(
                titleBox,
                new LinearLayout.LayoutParams(
                        0,
                        dp(52),
                        1
                )
        );

        /*
         * ARAMA
         */

        TextView searchButton =
                createTopIcon(
                        "⌕"
                );

        searchButton.setOnClickListener(
                v -> {

                    if (explorerSearch
                            .getVisibility()
                            == View.VISIBLE) {

                        explorerSearch
                                .setVisibility(
                                        View.GONE
                                );

                    } else {

                        explorerSearch
                                .setVisibility(
                                        View.VISIBLE
                                );

                        explorerSearch.requestFocus();
                    }
                }
        );

        topBar.addView(
                searchButton,
                new LinearLayout.LayoutParams(
                        dp(48),
                        dp(52)
                )
        );

        /*
         * GÖRÜNÜM
         */

        TextView viewButton =
                createTopIcon(
                        "☷"
                );

        viewButton.setOnClickListener(
                v -> Toast.makeText(
                        this,
                        "Liste görünümü",
                        Toast.LENGTH_SHORT
                ).show()
        );

        topBar.addView(
                viewButton,
                new LinearLayout.LayoutParams(
                        dp(48),
                        dp(52)
                )
        );

        /*
         * ÜÇ NOKTA
         */

        TextView moreButton =
                createTopIcon(
                        "⋮"
                );

        moreButton.setOnClickListener(
                v -> showExplorerMenu(
                        moreButton
                )
        );

        topBar.addView(
                moreButton,
                new LinearLayout.LayoutParams(
                        dp(44),
                        dp(52)
                )
        );

        explorerRoot.addView(
                topBar
        );

        /*
         * ========================================================
         * ARAMA ALANI
         * ========================================================
         */

        explorerSearch =
                new EditText(this);

        explorerSearch.setSingleLine(
                true
        );

        explorerSearch.setHint(
                "Dosya veya klasör ara..."
        );

        explorerSearch.setHintTextColor(
                Color.rgb(
                        150,
                        155,
                        165
                )
        );

        explorerSearch.setTextColor(
                Color.WHITE
        );

        explorerSearch.setTextSize(
                15
        );

        explorerSearch.setPadding(
                dp(14),
                0,
                dp(14),
                0
        );

        GradientDrawable searchBackground =
                new GradientDrawable();

        searchBackground.setColor(
                Color.rgb(
                        38,
                        42,
                        52
                )
        );

        searchBackground.setCornerRadius(
                dp(6)
        );

        explorerSearch.setBackground(
                searchBackground
        );

        explorerSearch.setVisibility(
                View.GONE
        );

        explorerSearch.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {

                        refreshExplorer();
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }
                }
        );

        explorerRoot.addView(
                explorerSearch,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(48)
                )
        );

        /*
         * ========================================================
         * DOSYA LİSTESİ
         * ========================================================
         */

        ScrollView scrollView =
                new ScrollView(this);

        scrollView.setFillViewport(
                true
        );

        explorerListContainer =
                new LinearLayout(this);

        explorerListContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        explorerListContainer.setPadding(
                dp(6),
                dp(4),
                dp(6),
                dp(90)
        );

        scrollView.addView(
                explorerListContainer,
                new ScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        explorerRoot.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        /*
         * ========================================================
         * ALT / + BUTONU
         * ========================================================
         */

        FrameLayout bottomFrame =
                new FrameLayout(this);

        bottomFrame.setBackgroundColor(
                Color.TRANSPARENT
        );

        TextView plus =
                new TextView(this);

        plus.setText(
                "+"
        );

        plus.setTextColor(
                Color.WHITE
        );

        plus.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                32
        );

        plus.setGravity(
                Gravity.CENTER
        );

        GradientDrawable plusBackground =
                new GradientDrawable();

        plusBackground.setColor(
                Color.rgb(
                        91,
                        135,
                        245
                )
        );

        plusBackground.setShape(
                GradientDrawable.OVAL
        );

        plus.setBackground(
                plusBackground
        );

        plus.setElevation(
                dp(8)
        );

        FrameLayout.LayoutParams plusParams =
                new FrameLayout.LayoutParams(
                        dp(66),
                        dp(66)
                );

        plusParams.gravity =
                Gravity.END
                        | Gravity.BOTTOM;

        plusParams.setMargins(
                0,
                0,
                dp(20),
                dp(18)
        );

        bottomFrame.addView(
                plus,
                plusParams
        );

        plus.setOnClickListener(
                v -> showCreateMenu(
                        explorerDirectory
                )
        );

        explorerRoot.addView(
                bottomFrame,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(86)
                )
        );

        /*
         * DIALOG
         */

        fileExplorerDialog.setContentView(
                explorerRoot
        );

        fileExplorerDialog.setOnDismissListener(
                dialog -> {

                    fileExplorerDialog =
                            null;

                    explorerRoot =
                            null;

                    explorerListContainer =
                            null;
                }
        );

        fileExplorerDialog.show();

        Window window =
                fileExplorerDialog.getWindow();

        if (window != null) {

            window.setBackgroundDrawableResource(
                    android.R.color.transparent
            );

            WindowManager.LayoutParams params =
                    new WindowManager.LayoutParams();

            params.copyFrom(
                    window.getAttributes()
            );

            params.width =
                    (int) (
                            getResources()
                                    .getDisplayMetrics()
                                    .widthPixels
                                    * 0.97f
                    );

            params.height =
                    (int) (
                            getResources()
                                    .getDisplayMetrics()
                                    .heightPixels
                                    * 0.90f
                    );

            window.setAttributes(
                    params
            );
        }

        refreshExplorer();
    }

    /*
     * ============================================================
     * DOSYA GEZGİNİ YENİLE
     * ============================================================
     */

    private void refreshExplorer() {

        if (explorerListContainer == null
                || explorerDirectory == null) {

            return;
        }

        explorerListContainer.removeAllViews();

        explorerTitle.setText(
                explorerDirectory.equals(repoDir)
                        ? repoDir.getName()
                        : explorerDirectory.getName()
        );

        explorerPath.setText(
                getShortExplorerPath(
                        explorerDirectory
                )
        );

        explorerItems =
                getDirectoryItems(
                        explorerDirectory
                );

        String searchText =
                explorerSearch == null
                        ? ""
                        : explorerSearch
                        .getText()
                        .toString()
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        /*
         * ÜST KLASÖR
         */

        if (!explorerDirectory.equals(
                repoDir
        )) {

            View parentRow =
                    createParentRow();

            explorerListContainer.addView(
                    parentRow
            );
        }

        /*
         * DOSYALAR
         */

        for (ExplorerItem item :
                explorerItems) {

            if (!searchText.isEmpty()
                    && !item.getName()
                    .toLowerCase(
                            Locale.ROOT
                    )
                    .contains(searchText)) {

                continue;
            }

            View row =
                    createExplorerRow(
                            item
                    );

            explorerListContainer.addView(
                    row
            );
        }

        if (explorerListContainer
                .getChildCount() == 0) {

            TextView empty =
                    new TextView(this);

            empty.setText(
                    "Bu klasör boş"
            );

            empty.setTextColor(
                    Color.rgb(
                            150,
                            155,
                            165
                    )
            );

            empty.setTextSize(
                    16
            );

            empty.setGravity(
                    Gravity.CENTER
            );

            empty.setPadding(
                    dp(20),
                    dp(80),
                    dp(20),
                    dp(80)
            );

            explorerListContainer.addView(
                    empty
            );
        }
    }

    /*
     * ============================================================
     * ".." SATIRI
     * ============================================================
     */

    private View createParentRow() {

        LinearLayout row =
                createExplorerBaseRow();

        TextView icon =
                createFileIcon(
                        "↑",
                        Color.rgb(
                                255,
                                170,
                                30
                        )
                );

        row.addView(
                icon,
                new LinearLayout.LayoutParams(
                        dp(82),
                        dp(64)
                )
        );

        LinearLayout textBox =
                new LinearLayout(this);

        textBox.setOrientation(
                LinearLayout.VERTICAL
        );

        TextView name =
                createRowName(
                        ".."
                );

        TextView info =
                createRowInfo(
                        "Üst klasör"
                );

        textBox.addView(
                name
        );

        textBox.addView(
                info
        );

        row.addView(
                textBox,
                new LinearLayout.LayoutParams(
                        0,
                        dp(64),
                        1
                )
        );

        row.setOnClickListener(
                v -> {

                    File parent =
                            explorerDirectory
                                    .getParentFile();

                    if (parent != null
                            && isInsideProject(parent)) {

                        explorerDirectory =
                                parent;

                        refreshExplorer();
                    }
                }
        );

        return row;
    }

    /*
     * ============================================================
     * DOSYA/KLASÖR SATIRI
     * ============================================================
     */

    private View createExplorerRow(
            ExplorerItem item
    ) {

        LinearLayout row =
                createExplorerBaseRow();

        String iconText;
        int iconColor;

        if (item.isFolder()) {

            iconText =
                    "📁";

            iconColor =
                    Color.rgb(
                            255,
                            165,
                            25
                    );

        } else {

            iconText =
                    getExplorerFileIcon(
                            item
                    );

            iconColor =
                    Color.rgb(
                            225,
                            230,
                            240
                    );
        }

        TextView icon =
                createFileIcon(
                        iconText,
                        iconColor
                );

        row.addView(
                icon,
                new LinearLayout.LayoutParams(
                        dp(82),
                        dp(70)
                )
        );

        LinearLayout textBox =
                new LinearLayout(this);

        textBox.setOrientation(
                LinearLayout.VERTICAL
        );

        textBox.setGravity(
                Gravity.CENTER_VERTICAL
        );

        TextView name =
                createRowName(
                        item.getName()
                );

        TextView info =
                createRowInfo(
                        getItemInfo(item)
                );

        textBox.addView(
                name
        );

        textBox.addView(
                info
        );

        row.addView(
                textBox,
                new LinearLayout.LayoutParams(
                        0,
                        dp(70),
                        1
                )
        );

        TextView date =
                new TextView(this);

        date.setText(
                formatDate(
                        item.getLastModified()
                )
        );

        date.setTextColor(
                Color.rgb(
                        160,
                        165,
                        175
                )
        );

        date.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                12
        );

        date.setGravity(
                Gravity.CENTER_VERTICAL
                        | Gravity.END
        );

        date.setPadding(
                dp(4),
                0,
                dp(8),
                0
        );

        row.addView(
                date,
                new LinearLayout.LayoutParams(
                        dp(92),
                        dp(70)
                )
        );

        row.setOnClickListener(
                v -> {

                    if (item.isFolder()) {

                        explorerDirectory =
                                item.getFile();

                        refreshExplorer();

                    } else {

                        if (fileExplorerDialog != null) {

                            fileExplorerDialog.dismiss();
                        }

                        openFileInEditor(
                                item.getFile()
                        );
                    }
                }
        );

        row.setOnLongClickListener(
                v -> {

                    showFileItemMenu(
                            item,
                            row
                    );

                    return true;
                }
        );

        return row;
    }

    /*
     * ============================================================
     * SATIR ANA KUTUSU
     * ============================================================
     */

    private LinearLayout createExplorerBaseRow() {

        LinearLayout row =
                new LinearLayout(this);

        row.setOrientation(
                LinearLayout.HORIZONTAL
        );

        row.setGravity(
                Gravity.CENTER_VERTICAL
        );

        row.setPadding(
                dp(6),
                dp(2),
                dp(4),
                dp(2)
        );

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                Color.rgb(
                        20,
                        23,
                        32
                )
        );

        background.setCornerRadius(
                dp(4)
        );

        row.setBackground(
                background
        );

        row.setMinimumHeight(
                dp(70)
        );

        return row;
    }

    /*
     * ============================================================
     * İKON
     * ============================================================
     */

    private TextView createFileIcon(
            String text,
            int color
    ) {

        TextView icon =
                new TextView(this);

        icon.setText(
                text
        );

        icon.setTextColor(
                color
        );

        icon.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                38
        );

        icon.setGravity(
                Gravity.CENTER
        );

        return icon;
    }

    /*
     * ============================================================
     * DOSYA ADI
     * ============================================================
     */

    private TextView createRowName(
            String text
    ) {

        TextView name =
                new TextView(this);

        name.setText(
                text
        );

        name.setTextColor(
                Color.WHITE
        );

        name.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                17
        );

        name.setMaxLines(
                1
        );

        name.setEllipsize(
                android.text.TextUtils.TruncateAt.END
        );

        return name;
    }

    /*
     * ============================================================
     * BİLGİ
     * ============================================================
     */

    private TextView createRowInfo(
            String text
    ) {

        TextView info =
                new TextView(this);

        info.setText(
                text
        );

        info.setTextColor(
                Color.rgb(
                        160,
                        165,
                        175
                )
        );

        info.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                12
        );

        info.setMaxLines(
                1
        );

        return info;
    }

    /*
     * ============================================================
     * DOSYA BİLGİSİ
     * ============================================================
     */

    private String getItemInfo(
            ExplorerItem item
    ) {

        if (item.isFolder()) {

            File[] files =
                    item.getFile()
                            .listFiles();

            if (files == null
                    || files.length == 0) {

                return "Boş";
            }

            int count = 0;

            for (File file : files) {

                if (file != null
                        && !file.isHidden()) {

                    count++;
                }
            }

            return count + " öğe";
        }

        return formatFileSize(
                item.getSize()
        );
    }

    /*
     * ============================================================
     * DOSYA İKONU
     * ============================================================
     */

    private String getExplorerFileIcon(
            ExplorerItem item
    ) {

        String ext =
                item.getExtension()
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (ext.equals("zip")
                || ext.equals("rar")
                || ext.equals("7z")
                || ext.equals("jar")
                || ext.equals("aar")) {

            return "📦";
        }

        if (ext.equals("java")) {
            return "☕";
        }

        if (ext.equals("kt")) {
            return "K";
        }

        if (ext.equals("xml")) {
            return "📄";
        }

        if (ext.equals("json")) {
            return "{}";
        }

        if (ext.equals("gradle")) {
            return "⚙";
        }

        if (ext.equals("png")
                || ext.equals("jpg")
                || ext.equals("jpeg")
                || ext.equals("webp")
                || ext.equals("gif")) {

            return "🖼";
        }

        if (ext.equals("mp4")
                || ext.equals("mkv")
                || ext.equals("avi")) {

            return "▶";
        }

        if (ext.equals("pdf")) {
            return "PDF";
        }

        if (ext.equals("doc")
                || ext.equals("docx")) {

            return "DOC";
        }

        return "📄";
    }

    /*
     * ============================================================
     * + OLUŞTUR
     * ============================================================
     */

    private void showCreateMenu(
            File directory
    ) {

        final Dialog createDialog =
                new Dialog(this);

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                dp(20),
                dp(16),
                dp(20),
                dp(16)
        );

        root.setBackgroundColor(
                Color.rgb(
                        48,
                        48,
                        80
                )
        );

        TextView title =
                new TextView(this);

        title.setText(
                "Oluştur"
        );

        title.setTextColor(
                Color.rgb(
                        180,
                        195,
                        255
                )
        );

        title.setTextSize(
                15
        );

        title.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        title.setPadding(
                dp(8),
                dp(4),
                dp(8),
                dp(16)
        );

        root.addView(
                title
        );

        TextView folder =
                createCreateOption(
                        "📁",
                        "Yeni klasör"
                );

        TextView file =
                createCreateOption(
                        "📄",
                        "Yeni dosya"
                );

        root.addView(
                folder
        );

        root.addView(
                file
        );

        folder.setOnClickListener(
                v -> {

                    createDialog.dismiss();

                    showCreateFolderDialog(
                            directory
                    );
                }
        );

        file.setOnClickListener(
                v -> {

                    createDialog.dismiss();

                    showCreateFileDialog(
                            directory
                    );
                }
        );

        createDialog.setContentView(
                root
        );

        createDialog.show();

        Window window =
                createDialog.getWindow();

        if (window != null) {

            window.setBackgroundDrawableResource(
                    android.R.color.transparent
            );

            WindowManager.LayoutParams params =
                    window.getAttributes();

            params.width =
                    dp(300);

            params.height =
                    WindowManager.LayoutParams.WRAP_CONTENT;

            window.setAttributes(
                    params
            );
        }
    }

    /*
     * ============================================================
     * OLUŞTURMA SEÇENEĞİ
     * ============================================================
     */

    private TextView createCreateOption(
            String icon,
            String text
    ) {

        TextView option =
                new TextView(this);

        option.setText(
                icon + "    " + text
        );

        option.setTextColor(
                Color.WHITE
        );

        option.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                18
        );

        option.setGravity(
                Gravity.CENTER_VERTICAL
        );

        option.setPadding(
                dp(8),
                dp(12),
                dp(8),
                dp(12)
        );

        return option;
    }

    /*
     * ============================================================
     * YENİ KLASÖR
     * ============================================================
     */

    private void showCreateFolderDialog(
            File directory
    ) {

        final EditText input =
                new EditText(this);

        input.setHint(
                "Klasör adı"
        );

        input.setSingleLine(
                true
        );

        new AlertDialog.Builder(this)
                .setTitle(
                        "Yeni klasör"
                )
                .setView(
                        input
                )
                .setNegativeButton(
                        "İptal",
                        null
                )
                .setPositiveButton(
                        "Oluştur",
                        null
                )
                .create()
                .show();

        /*
         * Pozitif butonun yanlış isimle boş klasör
         * oluşturmaması için ayrı listener gerekiyor.
         */

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "Yeni klasör"
                        )
                        .setView(
                                input
                        )
                        .setNegativeButton(
                                "İptal",
                                null
                        )
                        .setPositiveButton(
                                "Oluştur",
                                null
                        )
                        .create();

        dialog.setOnShowListener(
                d -> {

                    dialog.getButton(
                            AlertDialog.BUTTON_POSITIVE
                    ).setOnClickListener(
                            v -> {

                                String name =
                                        input.getText()
                                                .toString()
                                                .trim();

                                if (!isValidName(
                                        name
                                )) {

                                    input.setError(
                                            "Geçersiz klasör adı"
                                    );

                                    return;
                                }

                                File newFolder =
                                        new File(
                                                directory,
                                                name
                                        );

                                if (!isInsideProject(
                                        newFolder
                                )) {

                                    input.setError(
                                            "Geçersiz klasör yolu"
                                    );

                                    return;
                                }

                                if (newFolder.exists()) {

                                    input.setError(
                                            "Bu isim zaten var"
                                    );

                                    return;
                                }

                                if (newFolder.mkdirs()) {

                                    dialog.dismiss();

                                    Toast.makeText(
                                            this,
                                            "Klasör oluşturuldu",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    refreshExplorer();

                                } else {

                                    input.setError(
                                            "Klasör oluşturulamadı"
                                    );
                                }
                            }
                    );
                }
        );

        dialog.show();
    }

    /*
     * ============================================================
     * YENİ DOSYA
     * ============================================================
     */

    private void showCreateFileDialog(
            File directory
    ) {

        final EditText input =
                new EditText(this);

        input.setHint(
                "Örneğin MainActivity.java"
        );

        input.setSingleLine(
                true
        );

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "Yeni dosya"
                        )
                        .setView(
                                input
                        )
                        .setNegativeButton(
                                "İptal",
                                null
                        )
                        .setPositiveButton(
                                "Oluştur",
                                null
                        )
                        .create();

        dialog.setOnShowListener(
                d -> {

                    dialog.getButton(
                            AlertDialog.BUTTON_POSITIVE
                    ).setOnClickListener(
                            v -> {

                                String name =
                                        input.getText()
                                                .toString()
                                                .trim();

                                if (!isValidFileName(
                                        name
                                )) {

                                    input.setError(
                                            "Geçersiz dosya adı"
                                    );

                                    return;
                                }

                                File newFile =
                                        new File(
                                                directory,
                                                name
                                        );

                                if (!isInsideProject(
                                        newFile
                                )) {

                                    input.setError(
                                            "Geçersiz dosya yolu"
                                    );

                                    return;
                                }

                                if (newFile.exists()) {

                                    input.setError(
                                            "Bu dosya zaten var"
                                    );

                                    return;
                                }

                                try {

                                    if (newFile.createNewFile()) {

                                        dialog.dismiss();

                                        Toast.makeText(
                                                this,
                                                "Dosya oluşturuldu",
                                                Toast.LENGTH_SHORT
                                        ).show();

                                        refreshExplorer();

                                        openFileInEditor(
                                                newFile
                                        );

                                    } else {

                                        input.setError(
                                                "Dosya oluşturulamadı"
                                        );
                                    }

                                } catch (Exception e) {

                                    input.setError(
                                            "Dosya oluşturulamadı"
                                    );
                                }
                            }
                    );
                }
        );

        dialog.show();
    }

    /*
     * ============================================================
     * DOSYA MENÜSÜ
     * ============================================================
     */

    private void showFileItemMenu(
            ExplorerItem item,
            View anchor
    ) {

        PopupMenu menu =
                new PopupMenu(
                        this,
                        anchor
                );

        menu.getMenu().add(
                "Aç"
        );

        menu.getMenu().add(
                "Yeniden adlandır"
        );

        menu.getMenu().add(
                "Sil"
        );

        menu.setOnMenuItemClickListener(
                menuItem -> {

                    String action =
                            menuItem.getTitle()
                                    .toString();

                    if (action.equals(
                            "Aç"
                    )) {

                        if (item.isFolder()) {

                            explorerDirectory =
                                    item.getFile();

                            refreshExplorer();

                        } else {

                            if (fileExplorerDialog != null) {

                                fileExplorerDialog.dismiss();
                            }

                            openFileInEditor(
                                    item.getFile()
                            );
                        }

                        return true;
                    }

                    if (action.equals(
                            "Sil"
                    )) {

                        confirmDelete(
                                item
                        );

                        return true;
                    }

                    if (action.equals(
                            "Yeniden adlandır"
                    )) {

                        showRenameDialog(
                                item
                        );

                        return true;
                    }

                    return true;
                }
        );

        menu.show();
    }

    /*
     * ============================================================
     * YENİDEN ADLANDIR
     * ============================================================
     */

    private void showRenameDialog(
            ExplorerItem item
    ) {

        final EditText input =
                new EditText(this);

        input.setText(
                item.getName()
        );

        input.setSingleLine(
                true
        );

        new AlertDialog.Builder(this)
                .setTitle(
                        "Yeniden adlandır"
                )
                .setView(
                        input
                )
                .setNegativeButton(
                        "İptal",
                        null
                )
                .setPositiveButton(
                        "Kaydet",
                        (dialog, which) -> {

                            String name =
                                    input.getText()
                                            .toString()
                                            .trim();

                            if (!isValidName(
                                    name
                            )) {

                                Toast.makeText(
                                        this,
                                        "Geçersiz isim",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            File oldFile =
                                    item.getFile();

                            File newFile =
                                    new File(
                                            oldFile.getParentFile(),
                                            name
                                    );

                            if (!isInsideProject(
                                    newFile
                            )) {

                                Toast.makeText(
                                        this,
                                        "Geçersiz yol",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            if (newFile.exists()) {

                                Toast.makeText(
                                        this,
                                        "Bu isim zaten var",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            if (oldFile.renameTo(
                                    newFile
                            )) {

                                Toast.makeText(
                                        this,
                                        "Yeniden adlandırıldı",
                                        Toast.LENGTH_SHORT
                                ).show();

                                refreshExplorer();

                            } else {

                                Toast.makeText(
                                        this,
                                        "Yeniden adlandırılamadı",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                )
                .show();
    }

    /*
     * ============================================================
     * SİL
     * ============================================================
     */

    private void confirmDelete(
            ExplorerItem item
    ) {

        new AlertDialog.Builder(this)
                .setTitle(
                        "Sil"
                )
                .setMessage(
                        "\"" +
                                item.getName()
                                +
                                "\" silinsin mi?"
                )
                .setNegativeButton(
                        "İptal",
                        null
                )
                .setPositiveButton(
                        "Sil",
                        (dialog, which) -> {

                            if (deleteRecursively(
                                    item.getFile()
                            )) {

                                Toast.makeText(
                                        this,
                                        "Silindi",
                                        Toast.LENGTH_SHORT
                                ).show();

                                refreshExplorer();

                            } else {

                                Toast.makeText(
                                        this,
                                        "Silinemedi",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                )
                .show();
    }

    /*
     * ============================================================
     * KLASÖR / DOSYA SİL
     * ============================================================
     */

    private boolean deleteRecursively(
            File file
    ) {

        if (file == null
                || !isInsideProject(file)) {

            return false;
        }

        if (file.isDirectory()) {

            File[] children =
                    file.listFiles();

            if (children != null) {

                for (File child :
                        children) {

                    if (!deleteRecursively(
                            child
                    )) {

                        return false;
                    }
                }
            }
        }

        return file.delete();
    }

    /*
     * ============================================================
     * ÜST MENÜ
     * ============================================================
     */

    private void showExplorerMenu(
            View anchor
    ) {

        PopupMenu popup =
                new PopupMenu(
                        this,
                        anchor
                );

        popup.getMenu().add(
                "Görünüm"
        );

        popup.getMenu().add(
                "Gizli dosyalar"
        );

        popup.getMenu().add(
                "Yenile"
        );

        popup.getMenu().add(
                "Kapat"
        );

        popup.setOnMenuItemClickListener(
                item -> {

                    String action =
                            item.getTitle()
                                    .toString();

                    if (action.equals(
                            "Yenile"
                    )) {

                        refreshExplorer();

                    } else if (action.equals(
                            "Kapat"
                    )) {

                        if (fileExplorerDialog != null) {

                            fileExplorerDialog.dismiss();
                        }

                    } else {

                        Toast.makeText(
                                this,
                                action
                                        + " daha sonra bağlanacak",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    return true;
                }
        );

        popup.show();
    }

    /*
     * ============================================================
     * TOP ICON
     * ============================================================
     */

    private TextView createTopIcon(
            String text
    ) {

        TextView view =
                new TextView(this);

        view.setText(
                text
        );

        view.setTextColor(
                Color.WHITE
        );

        view.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                32
        );

        view.setGravity(
                Gravity.CENTER
        );

        return view;
    }

    /*
     * ============================================================
     * DOSYALARI AL
     * ============================================================
     */

    private ArrayList<ExplorerItem> getDirectoryItems(
            File directory
    ) {

        ArrayList<ExplorerItem> result =
                new ArrayList<>();

        File[] files =
                directory.listFiles();

        if (files == null) {
            return result;
        }

        for (File file :
                files) {

            if (file == null
                    || file.isHidden()) {

                continue;
            }

            result.add(
                    new ExplorerItem(
                            file
                    )
            );
        }

        Collections.sort(
                result,
                new Comparator<ExplorerItem>() {

                    @Override
                    public int compare(
                            ExplorerItem a,
                            ExplorerItem b
                    ) {

                        if (a.isFolder()
                                && !b.isFolder()) {

                            return -1;
                        }

                        if (!a.isFolder()
                                && b.isFolder()) {

                            return 1;
                        }

                        return a.getName()
                                .compareToIgnoreCase(
                                        b.getName()
                                );
                    }
                }
        );

        return result;
    }

    /*
     * ============================================================
     * DOSYA AÇ
     * ============================================================
     */

    private void openFileInEditor(
            File file
    ) {

        if (file == null
                || !file.exists()
                || !file.isFile()) {

            Toast.makeText(
                    this,
                    "Dosya bulunamadı",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (!isInsideProject(
                file
        )) {

            Toast.makeText(
                    this,
                    "Dosya proje dışında",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (file.length()
                > 5L * 1024L * 1024L) {

            Toast.makeText(
                    this,
                    "Bu dosya çok büyük",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        try {

            String content =
                    readFile(
                            file
                    );

            currentFile =
                    file;

            String relative =
                    getRelativePath(
                            file
                    );

            etFilePath.setText(
                    relative
            );

            etCode.setText(
                    content
            );

            etCode.setSelection(
                    etCode.length()
            );

            setStatus(
                    "Açıldı: "
                            + relative
            );

            if (file.getName()
                    .toLowerCase(
                            Locale.ROOT
                    )
                    .endsWith(".xml")) {

                renderLiveXml(
                        content
                );

            } else {

                phoneCanvas.removeAllViews();
            }

            Toast.makeText(
                    this,
                    "Dosya açıldı",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Dosya okunamadı: "
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    /*
     * ============================================================
     * KAYDET
     * ============================================================
     */

    private void saveCurrentFile() {

        String path =
                etFilePath.getText()
                        .toString()
                        .trim();

        if (path.isEmpty()) {

            Toast.makeText(
                    this,
                    "Önce bir dosya seç",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        try {

            File file =
                    resolveProjectPath(
                            path
                    );

            if (file == null) {

                Toast.makeText(
                        this,
                        "Geçersiz dosya yolu",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (file.exists()
                    && file.isDirectory()) {

                Toast.makeText(
                        this,
                        "Bu yol bir klasör",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            File parent =
                    file.getParentFile();

            if (parent == null
                    || !isInsideProject(
                    parent
            )) {

                Toast.makeText(
                        this,
                        "Geçersiz dosya yolu",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (!parent.exists()
                    && !parent.mkdirs()) {

                Toast.makeText(
                        this,
                        "Klasör oluşturulamadı",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            try (
                    FileOutputStream fos =
                            new FileOutputStream(
                                    file
                            )
            ) {

                fos.write(
                        etCode.getText()
                                .toString()
                                .getBytes(
                                        StandardCharsets.UTF_8
                                )
                );
            }

            currentFile =
                    file;

            setStatus(
                    "Kaydedildi: "
                            + getRelativePath(
                            file
                    )
            );

            Toast.makeText(
                    this,
                    "Dosya kaydedildi",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Kaydetme hatası: "
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    /*
     * ============================================================
     * DOSYA OKU
     * ============================================================
     */

    private String readFile(
            File file
    ) throws Exception {

        StringBuilder builder =
                new StringBuilder();

        try (
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        new FileInputStream(
                                                file
                                        ),
                                        StandardCharsets.UTF_8
                                )
                )
        ) {

            String line;

            while (
                    (line = reader.readLine())
                            != null
            ) {

                builder
                        .append(line)
                        .append('\n');
            }
        }

        return builder.toString();
    }

    /*
     * ============================================================
     * GÜVENLİ YOL
     * ============================================================
     */

    private File resolveProjectPath(
            String relativePath
    ) {

        if (relativePath == null) {
            return null;
        }

        String clean =
                relativePath
                        .trim()
                        .replace(
                                '\\',
                                '/'
                        );

        while (
                clean.startsWith("/")
        ) {

            clean =
                    clean.substring(
                            1
                    );
        }

        if (clean.isEmpty()) {
            return null;
        }

        File result =
                new File(
                        repoDir,
                        clean
                );

        return isInsideProject(
                result
        )
                ? result
                : null;
    }

    private boolean isInsideProject(
            File file
    ) {

        if (file == null
                || repoDir == null) {

            return false;
        }

        try {

            String root =
                    repoDir
                            .getCanonicalPath();

            String target =
                    file
                            .getCanonicalPath();

            return target.equals(root)
                    || target.startsWith(
                    root
                            + File.separator
            );

        } catch (Exception e) {

            return false;
        }
    }

    /*
     * ============================================================
     * GÖRECELİ YOL
     * ============================================================
     */

    private String getRelativePath(
            File file
    ) {

        try {

            String root =
                    repoDir
                            .getCanonicalPath();

            String target =
                    file
                            .getCanonicalPath();

            if (target.equals(
                    root
            )) {

                return "/";
            }

            String relative =
                    target.substring(
                            root.length()
                    );

            if (relative.startsWith(
                    File.separator
            )) {

                relative =
                        relative.substring(
                                1
                        );
            }

            return relative.replace(
                    File.separator,
                    "/"
            );

        } catch (Exception e) {

            return file.getName();
        }
    }

    /*
     * ============================================================
     * KISA YOL
     * ============================================================
     */

    private String getShortExplorerPath(
            File directory
    ) {

        String relative =
                getRelativePath(
                        directory
                );

        if (relative.equals("/")) {

            return repoDir.getName();
        }

        return repoDir.getName()
                + "/"
                + relative;
    }

    /*
     * ============================================================
     * İSİM KONTROLÜ
     * ============================================================
     */

    private boolean isValidName(
            String name
    ) {

        if (name == null
                || name.trim().isEmpty()) {

            return false;
        }

        if (name.equals(".")
                || name.equals("..")) {

            return false;
        }

        String invalid =
                "\\/:*?\"<>|";

        for (int i = 0;
             i < invalid.length();
             i++) {

            if (name.indexOf(
                    invalid.charAt(i)
            ) >= 0) {

                return false;
            }
        }

        return true;
    }

    private boolean isValidFileName(
            String name
    ) {

        return isValidName(
                name
        );
    }

    /*
     * ============================================================
     * DOSYA BOYUTU
     * ============================================================
     */

    private String formatFileSize(
            long bytes
    ) {

        if (bytes < 1024) {

            return bytes + " B";
        }

        if (bytes < 1024 * 1024) {

            return String.format(
                    Locale.ROOT,
                    "%.2f KB",
                    bytes / 1024.0
            );
        }

        if (bytes < 1024L * 1024L * 1024L) {

            return String.format(
                    Locale.ROOT,
                    "%.2f MB",
                    bytes / (
                            1024.0
                                    * 1024.0
                    )
            );
        }

        return String.format(
                Locale.ROOT,
                "%.2f GB",
                bytes / (
                        1024.0
                                * 1024.0
                                * 1024.0
                )
        );
    }

    /*
     * ============================================================
     * TARİH
     * ============================================================
     */

    private String formatDate(
            long time
    ) {

        if (time <= 0) {

            return "";
        }

        java.text.SimpleDateFormat format =
                new java.text.SimpleDateFormat(
                        "dd.MM.yyyy",
                        Locale.getDefault()
                );

        return format.format(
                new java.util.Date(
                        time
                )
        );
    }

    /*
     * ============================================================
     * STATUS
     * ============================================================
     */

    private void setStatus(
            String text
    ) {

        if (txtStatus != null) {

            txtStatus.setText(
                    text
            );
        }
    }

    /*
     * ============================================================
     * XML ÖNİZLEME
     * ============================================================
     */

    private void renderLiveXml(
            String xmlCode
    ) {

        if (xmlCode == null
                || xmlCode.trim().isEmpty()) {

            phoneCanvas.removeAllViews();

            return;
        }

        if (currentFile != null
                && !currentFile.getName()
                .toLowerCase(
                        Locale.ROOT
                )
                .endsWith(".xml")) {

            return;
        }

        try {

            XmlPullParserFactory factory =
                    XmlPullParserFactory
                            .newInstance();

            factory.setNamespaceAware(
                    true
            );

            XmlPullParser parser =
                    factory.newPullParser();

            parser.setInput(
                    new java.io.StringReader(
                            xmlCode
                    )
            );

            phoneCanvas.removeAllViews();

            ArrayList<View> stack =
                    new ArrayList<>();

            int event;

            while (
                    (event = parser.next())
                            != XmlPullParser.END_DOCUMENT
            ) {

                if (event ==
                        XmlPullParser.START_TAG) {

                    View view =
                            createPreviewView(
                                    parser.getName(),
                                    parser
                            );

                    if (view == null) {
                        continue;
                    }

                    if (stack.isEmpty()) {

                        phoneCanvas.addView(
                                view,
                                createPreviewLayoutParams(
                                        view,
                                        null,
                                        parser
                                )
                        );

                    } else {

                        View parent =
                                stack.get(
                                        stack.size() - 1
                                );

                        if (parent instanceof ViewGroup) {

                            ((ViewGroup) parent)
                                    .addView(
                                            view,
                                            createPreviewLayoutParams(
                                                    view,
                                                    (ViewGroup) parent,
                                                    parser
                                            )
                                    );
                        }
                    }

                    if (view instanceof ViewGroup) {

                        stack.add(
                                view
                        );
                    }

                } else if (
                        event ==
                                XmlPullParser.END_TAG
                ) {

                    String tag =
                            parser.getName();

                    if (isContainerTag(
                            tag
                    )
                            && !stack.isEmpty()) {

                        stack.remove(
                                stack.size() - 1
                        );
                    }
                }
            }

            setStatus(
                    "Önizleme güncellendi"
            );

        } catch (Exception e) {

            showPreviewError(
                    e.getMessage()
            );
        }
    }

    private View createPreviewView(
            String tag,
            XmlPullParser parser
    ) {

        if (tag == null) {
            return null;
        }

        if (tag.contains(".")) {

            tag =
                    tag.substring(
                            tag.lastIndexOf(".")
                                    + 1
                    );
        }

        View view;

        switch (tag) {

            case "LinearLayout":

                LinearLayout linear =
                        new LinearLayout(
                                this
                        );

                String orientation =
                        getAttribute(
                                parser,
                                "orientation"
                        );

                linear.setOrientation(
                        "horizontal"
                                .equalsIgnoreCase(
                                        orientation
                                )
                                ? LinearLayout.HORIZONTAL
                                : LinearLayout.VERTICAL
                );

                view = linear;

                break;

            case "FrameLayout":

                view =
                        new FrameLayout(
                                this
                        );

                break;

            case "RelativeLayout":

                view =
                        new android.widget.RelativeLayout(
                                this
                        );

                break;

            case "TextView":

                TextView text =
                        new TextView(
                                this
                        );

                text.setText(
                        getAttribute(
                                parser,
                                "text"
                        )
                );

                text.setTextSize(
                        TypedValue.COMPLEX_UNIT_SP,
                        getTextSize(
                                parser
                        )
                );

                text.setTextColor(
                        Color.DKGRAY
                );

                view = text;

                break;

            case "Button":

                Button button =
                        new Button(
                                this
                        );

                String buttonText =
                        getAttribute(
                                parser,
                                "text"
                        );

                button.setText(
                        buttonText.isEmpty()
                                ? "Button"
                                : buttonText
                );

                view = button;

                break;

            case "EditText":

                EditText edit =
                        new EditText(
                                this
                        );

                edit.setHint(
                        getAttribute(
                                parser,
                                "hint"
                        )
                );

                edit.setText(
                        getAttribute(
                                parser,
                                "text"
                        )
                );

                view = edit;

                break;

            case "ImageView":

                android.widget.ImageView image =
                        new android.widget.ImageView(
                                this
                        );

                image.setBackgroundColor(
                        Color.LTGRAY
                );

                view = image;

                break;

            case "Space":

                view =
                        new android.widget.Space(
                                this
                        );

                break;

            default:

                return null;
        }

        applyCommonAttributes(
                view,
                parser
        );

        return view;
    }

    private void applyCommonAttributes(
            View view,
            XmlPullParser parser
    ) {

        String background =
                getAttribute(
                        parser,
                        "background"
                );

        if (background.startsWith("#")) {

            try {

                view.setBackgroundColor(
                        Color.parseColor(
                                background
                        )
                );

            } catch (Exception ignored) {
            }
        }

        String padding =
                getAttribute(
                        parser,
                        "padding"
                );

        if (!padding.isEmpty()) {

            int p =
                    parseDimension(
                            padding,
                            0
                    );

            view.setPadding(
                    p,
                    p,
                    p,
                    p
            );
        }
    }

    private ViewGroup.LayoutParams
    createPreviewLayoutParams(
            View view,
            ViewGroup parent,
            XmlPullParser parser
    ) {

        if (parent == null) {

            return new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }

        int width =
                parseLayoutSize(
                        getAttribute(
                                parser,
                                "layout_width"
                        )
                );

        int height =
                parseLayoutSize(
                        getAttribute(
                                parser,
                                "layout_height"
                        )
                );

        if (parent instanceof LinearLayout) {

            return new LinearLayout.LayoutParams(
                    width,
                    height
            );
        }

        if (parent instanceof FrameLayout) {

            return new FrameLayout.LayoutParams(
                    width,
                    height
            );
        }

        if (parent instanceof android.widget.RelativeLayout) {

            return new android.widget.RelativeLayout.LayoutParams(
                    width,
                    height
            );
        }

        return new ViewGroup.LayoutParams(
                width,
                height
        );
    }

    private int parseLayoutSize(
            String value
    ) {

        if (value == null
                || value.isEmpty()
                || value.equals(
                "wrap_content"
        )) {

            return ViewGroup.LayoutParams.WRAP_CONTENT;
        }

        if (value.equals(
                "match_parent"
        )) {

            return ViewGroup.LayoutParams.MATCH_PARENT;
        }

        return parseDimension(
                value,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private boolean isContainerTag(
            String tag
    ) {

        return "LinearLayout".equals(
                tag
        )
                || "FrameLayout".equals(
                tag
        )
                || "RelativeLayout".equals(
                tag
        );
    }

    private String getAttribute(
            XmlPullParser parser,
            String name
    ) {

        String value =
                parser.getAttributeValue(
                        "http://schemas.android.com/apk/res/android",
                        name
                );

        if (value == null) {

            value =
                    parser.getAttributeValue(
                            null,
                            name
                    );
        }

        return value == null
                ? ""
                : value;
    }

    private float getTextSize(
            XmlPullParser parser
    ) {

        String value =
                getAttribute(
                        parser,
                        "textSize"
                );

        if (value.isEmpty()) {
            return 14;
        }

        try {

            return Float.parseFloat(
                    value
                            .replace(
                                    "sp",
                                    ""
                            )
                            .trim()
            );

        } catch (Exception e) {

            return 14;
        }
    }

    private int parseDimension(
            String value,
            int defaultValue
    ) {

        if (value == null
                || value.isEmpty()) {

            return defaultValue;
        }

        try {

            String clean =
                    value
                            .replace(
                                    "dp",
                                    ""
                            )
                            .replace(
                                    "sp",
                                    ""
                            )
                            .trim();

            float number =
                    Float.parseFloat(
                            clean
                    );

            return (int)
                    TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            number,
                            getResources()
                                    .getDisplayMetrics()
                    );

        } catch (Exception e) {

            return defaultValue;
        }
    }

    private void showPreviewError(
            String message
    ) {

        phoneCanvas.removeAllViews();

        TextView error =
                new TextView(this);

        error.setText(
                "XML önizleme hatası\n\n"
                        + (
                        message == null
                                ? "Bilinmeyen hata"
                                : message
                )
        );

        error.setTextColor(
                Color.rgb(
                        220,
                        80,
                        80
                )
        );

        error.setGravity(
                Gravity.CENTER
        );

        error.setPadding(
                dp(12),
                dp(12),
                dp(12),
                dp(12)
        );

        phoneCanvas.addView(
                error,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        setStatus(
                "XML hatası"
        );
    }

    /*
     * ============================================================
     * DP
     * ============================================================
     */

    private int dp(
            int value
    ) {

        return (int)
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        value,
                        getResources()
                                .getDisplayMetrics()
                );
    }

    /*
     * ============================================================
     * ACTIVITY KAPANIRKEN
     * ============================================================
     */

    @Override
    protected void onDestroy() {

        if (previewRunnable != null) {

            previewHandler.removeCallbacks(
                    previewRunnable
            );
        }

        if (fileExplorerDialog != null
                && fileExplorerDialog.isShowing()) {

            fileExplorerDialog.dismiss();
        }

        super.onDestroy();
    }
}
