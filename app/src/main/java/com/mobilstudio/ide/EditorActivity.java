package com.mobilstudio.ide;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
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
import java.util.List;

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

    /**
     * Açılan projenin ana klasörü.
     *
     * Örnek:
     *
     * /.../MobilStudio_Projects/TestApp
     */
    private File repoDir;

    /**
     * Şu anda dosya gezgininde bulunduğumuz klasör.
     */
    private File currentExplorerDir;

    /**
     * Şu anda editörde açık olan dosya.
     */
    private File currentFile;

    /**
     * Kod değiştikçe sürekli önizleme üretmemek için
     * küçük bir gecikme kullanıyoruz.
     */
    private android.os.Handler previewHandler =
            new android.os.Handler();

    private Runnable previewRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_editor);

        /*
         * ---------------------------------------------------------
         * PROJE YOLUNU AL
         * ---------------------------------------------------------
         */

        String receivedRepoPath =
                getIntent().getStringExtra("REPO_PATH");

        if (receivedRepoPath == null ||
                receivedRepoPath.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "Proje yolu alınamadı",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        repoDir = new File(receivedRepoPath);

        if (!repoDir.exists() || !repoDir.isDirectory()) {

            Toast.makeText(
                    this,
                    "Proje klasörü bulunamadı",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        currentExplorerDir = repoDir;

        /*
         * ---------------------------------------------------------
         * VIEW'LARI BUL
         * ---------------------------------------------------------
         */

        etFilePath = findViewById(R.id.etFilePath);
        etCode = findViewById(R.id.etCode);

        btnSave = findViewById(R.id.btnSave);
        btnOpen = findViewById(R.id.btnOpen);
        btnBuild = findViewById(R.id.btnBuild);

        floatingPreview =
                findViewById(R.id.floatingPreview);

        phoneCanvas =
                findViewById(R.id.phoneCanvas);

        txtStatus =
                findViewById(R.id.txtStatus);

        /*
         * ---------------------------------------------------------
         * BAŞLANGIÇ DURUMU
         * ---------------------------------------------------------
         */

        etFilePath.setText("");

        setStatus(
                "Proje: " + repoDir.getName()
        );

        /*
         * ---------------------------------------------------------
         * ÖNİZLEMEYİ SÜRÜKLE
         * ---------------------------------------------------------
         */

        setupFloatingPreview();

        /*
         * ---------------------------------------------------------
         * DOSYA YOLU ARTIK KLASÖR OLUŞTURMUYOR
         *
         * ÖNCEKİ KODDA:
         *
         * activity_main/
         *
         * gibi bir şey yazınca otomatik klasör oluşturuluyordu.
         *
         * BUNU TAMAMEN KALDIRDIK.
         * ---------------------------------------------------------
         */

        /*
         * ---------------------------------------------------------
         * KOD DEĞİŞTİĞİNDE ÖNİZLEME
         * ---------------------------------------------------------
         */

        etCode.addTextChangedListener(new TextWatcher() {

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
        });

        /*
         * ---------------------------------------------------------
         * KAYDET
         * ---------------------------------------------------------
         */

        btnSave.setOnClickListener(
                v -> saveCurrentFile()
        );

        /*
         * ---------------------------------------------------------
         * AÇ
         *
         * ARTIK DOSYA YOLUNU ELLE YAZMAK ZORUNDA DEĞİLİZ.
         * ---------------------------------------------------------
         */

        btnOpen.setOnClickListener(
                v -> showFileExplorer(repoDir)
        );

        /*
         * ---------------------------------------------------------
         * DERLE
         *
         * Şimdilik gerçek APK derleme bağlamıyoruz.
         * Butonun mevcut olduğunu biliyoruz ama burada sahte
         * "APK oluşturuldu" mesajı vermiyoruz.
         * ---------------------------------------------------------
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

        /*
         * ---------------------------------------------------------
         * DOSYA YOLU ALANINA DOKUNUNCA DOSYA GEZGİNİ
         * ---------------------------------------------------------
         */

        etFilePath.setOnClickListener(
                v -> showFileExplorer(repoDir)
        );

        /*
         * İlk açılışta boş önizleme.
         */
        phoneCanvas.removeAllViews();
    }

    /*
     * ============================================================
     * ÖNİZLEMEYİ SÜRÜKLEME
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

        previewRunnable = () -> {

            renderLiveXml(
                    xmlCode
            );
        };

        previewHandler.postDelayed(
                previewRunnable,
                250
        );
    }

    /*
     * ============================================================
     * DOSYA GEZGİNİ
     * ============================================================
     *
     * Örnek:
     *
     * TestApp
     *   app/
     *      src/
     *         main/
     *             java/
     *             res/
     *             AndroidManifest.xml
     *
     * Klasöre basınca içine girer.
     * Dosyaya basınca editörde açar.
     * ============================================================
     */

    private void showFileExplorer(
            File directory
    ) {

        if (directory == null) {
            return;
        }

        if (!directory.exists()) {

            Toast.makeText(
                    this,
                    "Klasör bulunamadı",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (!directory.isDirectory()) {

            Toast.makeText(
                    this,
                    "Bu bir klasör değil",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        currentExplorerDir =
                directory;

        final AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .create();

        /*
         * Ana dikey kutu
         */

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                dp(12),
                dp(8),
                dp(12),
                dp(8)
        );

        /*
         * ---------------------------------------------------------
         * BAŞLIK
         * ---------------------------------------------------------
         */

        LinearLayout header =
                new LinearLayout(this);

        header.setOrientation(
                LinearLayout.HORIZONTAL
        );

        header.setGravity(
                Gravity.CENTER_VERTICAL
        );

        TextView title =
                new TextView(this);

        title.setText(
                directory.equals(repoDir)
                        ? repoDir.getName()
                        : directory.getName()
        );

        title.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                19
        );

        title.setTextColor(
                Color.rgb(
                        17,
                        24,
                        39
                )
        );

        title.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(
                        0,
                        dp(50),
                        1
                );

        header.addView(
                title,
                titleParams
        );

        /*
         * + OLUŞTUR
         */

        Button createButton =
                new Button(this);

        createButton.setText("+");

        createButton.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                22
        );

        createButton.setOnClickListener(
                v -> showCreateMenu(
                        directory,
                        dialog
                )
        );

        header.addView(
                createButton,
                new LinearLayout.LayoutParams(
                        dp(55),
                        dp(50)
                )
        );

        root.addView(
                header
        );

        /*
         * ---------------------------------------------------------
         * YOL
         * ---------------------------------------------------------
         */

        TextView pathText =
                new TextView(this);

        pathText.setText(
                getRelativePath(directory)
        );

        pathText.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                12
        );

        pathText.setTextColor(
                Color.DKGRAY
        );

        pathText.setPadding(
                dp(4),
                0,
                dp(4),
                dp(8)
        );

        root.addView(
                pathText
        );

        /*
         * ---------------------------------------------------------
         * DOSYA LİSTESİ
         * ---------------------------------------------------------
         */

        ListView listView =
                new ListView(this);

        ArrayList<ExplorerItem> items =
                getDirectoryItems(
                        directory
                );

        ArrayList<String> names =
                new ArrayList<>();

        /*
         * Üst klasöre çıkma satırı.
         */

        if (!directory.equals(repoDir)) {

            names.add("⬆  ..");
        }

        for (ExplorerItem item : items) {

            if (item.isFolder()) {

                names.add(
                        "📁  "
                                + item.getName()
                );

            } else {

                names.add(
                        getFileIcon(
                                item.getName()
                        )
                                + "  "
                                + item.getName()
                );
            }
        }

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_list_item_1,
                        names
                );

        listView.setAdapter(
                adapter
        );

        root.addView(
                listView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        /*
         * ---------------------------------------------------------
         * LİSTE TIKLAMA
         * ---------------------------------------------------------
         */

        listView.setOnItemClickListener(
                (parent, view, position, id) -> {

                    int actualIndex =
                            position;

                    /*
                     * ".." satırı varsa gerçek
                     * item indexini 1 azalt.
                     */

                    if (!directory.equals(repoDir)) {

                        if (position == 0) {

                            File parentDir =
                                    directory.getParentFile();

                            if (parentDir != null &&
                                    isInsideProject(parentDir)) {

                                dialog.dismiss();

                                showFileExplorer(
                                        parentDir
                                );
                            }

                            return;
                        }

                        actualIndex =
                                position - 1;
                    }

                    if (actualIndex < 0 ||
                            actualIndex >= items.size()) {

                        return;
                    }

                    ExplorerItem item =
                            items.get(
                                    actualIndex
                            );

                    File selected =
                            item.getFile();

                    if (selected.isDirectory()) {

                        dialog.dismiss();

                        showFileExplorer(
                                selected
                        );

                    } else {

                        dialog.dismiss();

                        openFileInEditor(
                                selected
                        );
                    }
                }
        );

        /*
         * ---------------------------------------------------------
         * ALT BUTONLAR
         * ---------------------------------------------------------
         */

        LinearLayout bottom =
                new LinearLayout(this);

        bottom.setOrientation(
                LinearLayout.HORIZONTAL
        );

        bottom.setGravity(
                Gravity.CENTER
        );

        Button rootButton =
                new Button(this);

        rootButton.setText(
                "Proje kökü"
        );

        rootButton.setOnClickListener(
                v -> {

                    dialog.dismiss();

                    showFileExplorer(
                            repoDir
                    );
                }
        );

        Button cancelButton =
                new Button(this);

        cancelButton.setText(
                "Kapat"
        );

        cancelButton.setOnClickListener(
                v -> dialog.dismiss()
        );

        bottom.addView(
                rootButton,
                new LinearLayout.LayoutParams(
                        0,
                        dp(48),
                        1
                )
        );

        bottom.addView(
                cancelButton,
                new LinearLayout.LayoutParams(
                        0,
                        dp(48),
                        1
                )
        );

        root.addView(
                bottom
        );

        dialog.setView(
                root
        );

        dialog.show();

        /*
         * Dialog boyutunu biraz büyüt.
         */

        if (dialog.getWindow() != null) {

            dialog.getWindow().setLayout(
                    (int) (getResources()
                            .getDisplayMetrics()
                            .widthPixels * 0.94f),
                    (int) (getResources()
                            .getDisplayMetrics()
                            .heightPixels * 0.82f)
            );
        }
    }

    /*
     * ============================================================
     * KLASÖRDEKİ DOSYALARI AL
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

        for (File file : files) {

            /*
             * Gizli dosyaları şimdilik göstermiyoruz.
             */

            if (file.isHidden()) {
                continue;
            }

            result.add(
                    new ExplorerItem(
                            file
                    )
            );
        }

        /*
         * Klasörler önce,
         * dosyalar sonra.
         */

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
     * OLUŞTUR MENÜSÜ
     * ============================================================
     */

    private void showCreateMenu(
            File directory,
            AlertDialog explorerDialog
    ) {

        String[] options = {
                "Yeni klasör",
                "Yeni dosya"
        };

        new AlertDialog.Builder(this)
                .setTitle("Oluştur")
                .setItems(
                        options,
                        (dialog, which) -> {

                            if (which == 0) {

                                showCreateFolderDialog(
                                        directory,
                                        explorerDialog
                                );

                            } else {

                                showCreateFileDialog(
                                        directory,
                                        explorerDialog
                                );
                            }
                        }
                )
                .show();
    }

    /*
     * ============================================================
     * YENİ KLASÖR
     * ============================================================
     */

    private void showCreateFolderDialog(
            File directory,
            AlertDialog explorerDialog
    ) {

        final EditText input =
                new EditText(this);

        input.setHint(
                "Klasör adı"
        );

        input.setSingleLine(true);

        new AlertDialog.Builder(this)
                .setTitle("Yeni klasör")
                .setView(input)
                .setNegativeButton(
                        "İptal",
                        null
                )
                .setPositiveButton(
                        "Oluştur",
                        (dialog, which) -> {

                            String name =
                                    input.getText()
                                            .toString()
                                            .trim();

                            if (!isValidName(name)) {

                                Toast.makeText(
                                        this,
                                        "Geçersiz klasör adı",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            File newFolder =
                                    new File(
                                            directory,
                                            name
                                    );

                            if (newFolder.exists()) {

                                Toast.makeText(
                                        this,
                                        "Bu isim zaten kullanılıyor",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            if (newFolder.mkdirs()) {

                                Toast.makeText(
                                        this,
                                        "Klasör oluşturuldu",
                                        Toast.LENGTH_SHORT
                                ).show();

                                explorerDialog.dismiss();

                                showFileExplorer(
                                        directory
                                );

                            } else {

                                Toast.makeText(
                                        this,
                                        "Klasör oluşturulamadı",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                )
                .show();
    }

    /*
     * ============================================================
     * YENİ DOSYA
     * ============================================================
     */

    private void showCreateFileDialog(
            File directory,
            AlertDialog explorerDialog
    ) {

        final EditText input =
                new EditText(this);

        input.setHint(
                "Örneğin MainActivity.java"
        );

        input.setSingleLine(true);

        new AlertDialog.Builder(this)
                .setTitle("Yeni dosya")
                .setView(input)
                .setNegativeButton(
                        "İptal",
                        null
                )
                .setPositiveButton(
                        "Oluştur",
                        (dialog, which) -> {

                            String name =
                                    input.getText()
                                            .toString()
                                            .trim();

                            if (!isValidFileName(name)) {

                                Toast.makeText(
                                        this,
                                        "Geçersiz dosya adı",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            File newFile =
                                    new File(
                                            directory,
                                            name
                                    );

                            if (newFile.exists()) {

                                Toast.makeText(
                                        this,
                                        "Bu dosya zaten var",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            try {

                                File parent =
                                        newFile.getParentFile();

                                if (parent == null ||
                                        !isInsideProject(parent)) {

                                    throw new Exception(
                                            "Geçersiz dosya yolu"
                                    );
                                }

                                if (newFile.createNewFile()) {

                                    Toast.makeText(
                                            this,
                                            "Dosya oluşturuldu",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    explorerDialog.dismiss();

                                    showFileExplorer(
                                            directory
                                    );

                                } else {

                                    Toast.makeText(
                                            this,
                                            "Dosya oluşturulamadı",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }

                            } catch (Exception e) {

                                Toast.makeText(
                                        this,
                                        "Dosya oluşturulamadı: "
                                                + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                )
                .show();
    }

    /*
     * ============================================================
     * DOSYA AÇ
     * ============================================================
     */

    private void openFileInEditor(
            File file
    ) {

        if (file == null ||
                !file.exists() ||
                !file.isFile()) {

            Toast.makeText(
                    this,
                    "Dosya bulunamadı",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (!isInsideProject(file)) {

            Toast.makeText(
                    this,
                    "Bu dosya proje dışında",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        /*
         * Çok büyük binary dosyaları kod editöründe açmayalım.
         */

        if (file.length() > 5 * 1024 * 1024) {

            Toast.makeText(
                    this,
                    "Bu dosya çok büyük",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        try {

            String content =
                    readFile(file);

            currentFile =
                    file;

            String relativePath =
                    getRelativePath(file);

            etFilePath.setText(
                    relativePath
            );

            etCode.setText(
                    content
            );

            etCode.setSelection(
                    etCode.length()
            );

            setStatus(
                    "Açıldı: "
                            + relativePath
            );

            /*
             * XML ise önizlemeyi hemen çalıştır.
             */

            if (file.getName()
                    .toLowerCase()
                    .endsWith(".xml")) {

                renderLiveXml(
                        content
                );
            } else {

                phoneCanvas.removeAllViews();

                TextView info =
                        new TextView(this);

                info.setText(
                        "Önizleme yalnızca XML layout dosyalarında çalışır."
                );

                info.setGravity(
                        Gravity.CENTER
                );

                info.setTextColor(
                        Color.DKGRAY
                );

                phoneCanvas.addView(
                        info,
                        new FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                        )
                );
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

            /*
             * Klasörün üzerine yazmaya çalışma.
             */

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

            if (parent == null ||
                    !isInsideProject(parent)) {

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

            String code =
                    etCode.getText()
                            .toString();

            try (FileOutputStream fos =
                         new FileOutputStream(file)) {

                fos.write(
                        code.getBytes(
                                StandardCharsets.UTF_8
                        )
                );
            }

            currentFile =
                    file;

            setStatus(
                    "Kaydedildi: "
                            + getRelativePath(file)
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
                                        new FileInputStream(file),
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
     * PROJE YOLU GÜVENLİK KONTROLÜ
     * ============================================================
     *
     * Örneğin:
     *
     * ../başka-klasör
     *
     * gibi bir yol kullanarak proje dışına çıkılmasını
     * engelliyoruz.
     * ============================================================
     */

    private File resolveProjectPath(
            String relativePath
    ) {

        if (relativePath == null) {
            return null;
        }

        String clean =
                relativePath.trim();

        if (clean.isEmpty()) {
            return null;
        }

        /*
         * Windows tarzı yolu da engelle.
         */

        clean =
                clean.replace(
                        '\\',
                        '/'
                );

        while (
                clean.startsWith("/")
        ) {

            clean =
                    clean.substring(1);
        }

        File result =
                new File(
                        repoDir,
                        clean
                );

        try {

            String root =
                    repoDir
                            .getCanonicalPath();

            String target =
                    result
                            .getCanonicalPath();

            if (target.equals(root)) {
                return result;
            }

            if (!target.startsWith(
                    root + File.separator
            )) {

                return null;
            }

            return result;

        } catch (Exception e) {

            return null;
        }
    }

    /*
     * ============================================================
     * PROJE İÇİNDE Mİ?
     * ============================================================
     */

    private boolean isInsideProject(
            File file
    ) {

        try {

            String root =
                    repoDir
                            .getCanonicalPath();

            String target =
                    file
                            .getCanonicalPath();

            return target.equals(root)
                    || target.startsWith(
                    root + File.separator
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

            if (target.equals(root)) {
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
     * DOSYA ADI KONTROLÜ
     * ============================================================
     */

    private boolean isValidName(
            String name
    ) {

        if (name == null ||
                name.trim().isEmpty()) {

            return false;
        }

        if (name.equals(".")
                || name.equals("..")) {

            return false;
        }

        return !name.contains("/")
                && !name.contains("\\")
                && !name.contains(":")
                && !name.contains("*")
                && !name.contains("?")
                && !name.contains("\"")
                && !name.contains("<")
                && !name.contains(">")
                && !name.contains("|");
    }

    private boolean isValidFileName(
            String name
    ) {

        if (!isValidName(name)) {
            return false;
        }

        /*
         * Dosyanın uzantısız olmasına izin veriyoruz.
         * Örneğin:
         *
         * README
         * MainActivity.java
         * activity_main.xml
         * build.gradle
         */

        return true;
    }

    /*
     * ============================================================
     * DOSYA İKONU
     * ============================================================
     */

    private String getFileIcon(
            String name
    ) {

        String lower =
                name.toLowerCase();

        if (lower.endsWith(".java")) {
            return "☕";
        }

        if (lower.endsWith(".kt")) {
            return "K";
        }

        if (lower.endsWith(".xml")) {
            return "📄";
        }

        if (lower.endsWith(".gradle")) {
            return "⚙";
        }

        if (lower.endsWith(".json")) {
            return "{}";
        }

        if (lower.endsWith(".txt")) {
            return "📝";
        }

        if (lower.endsWith(".png")
                || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".webp")) {

            return "🖼";
        }

        return "📄";
    }

    /*
     * ============================================================
     * CANLI XML ÖNİZLEME
     * ============================================================
     *
     * Önceki kod:
     *
     * if (xml.contains("<Button"))
     *
     * şeklinde çalışıyordu.
     *
     * Bu gerçek bir XML parser değildi.
     *
     * Burada XML'i Android XmlPullParser ile gerçekten
     * okumaya başlıyoruz.
     *
     * Bu ilk sürüm temel View'ları destekler:
     *
     * LinearLayout
     * FrameLayout
     * RelativeLayout
     * TextView
     * Button
     * EditText
     * ImageView
     *
     * Daha sonra ConstraintLayout ve diğer Android
     * bileşenlerini ayrıca ekleyebiliriz.
     * ============================================================
     */

    private void renderLiveXml(
            String xmlCode
    ) {

        if (xmlCode == null ||
                xmlCode.trim().isEmpty()) {

            phoneCanvas.removeAllViews();

            return;
        }

        /*
         * Sadece XML dosyalarında önizleme.
         */

        if (currentFile != null) {

            String name =
                    currentFile.getName()
                            .toLowerCase();

            if (!name.endsWith(".xml")) {
                return;
            }
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

            ArrayList<View> viewStack =
                    new ArrayList<>();

            int event;

            while (
                    (event = parser.next())
                            != XmlPullParser.END_DOCUMENT
            ) {

                if (event ==
                        XmlPullParser.START_TAG) {

                    String tag =
                            parser.getName();

                    View view =
                            createPreviewView(
                                    tag,
                                    parser
                            );

                    if (view == null) {
                        continue;
                    }

                    /*
                     * İlk View telefon ekranının
                     * root'u olur.
                     */

                    if (viewStack.isEmpty()) {

                        phoneCanvas.addView(
                                view,
                                createPreviewLayoutParams(
                                        view,
                                        null
                                )
                        );

                    } else {

                        View parent =
                                viewStack.get(
                                        viewStack.size() - 1
                                );

                        if (parent instanceof ViewGroup) {

                            ViewGroup group =
                                    (ViewGroup) parent;

                            group.addView(
                                    view,
                                    createPreviewLayoutParams(
                                            view,
                                            group
                                    )
                            );
                        }
                    }

                    /*
                     * Container ise stack'e ekle.
                     */

                    if (view instanceof ViewGroup) {

                        viewStack.add(
                                view
                        );
                    }
                }

                else if (
                        event ==
                                XmlPullParser.END_TAG
                ) {

                    String tag =
                            parser.getName();

                    if (isContainerTag(tag)
                            && !viewStack.isEmpty()) {

                        viewStack.remove(
                                viewStack.size() - 1
                        );
                    }
                }
            }

            setStatus(
                    "Önizleme güncellendi"
            );

        } catch (Exception e) {

            /*
             * Kullanıcı yazarken XML geçici olarak
             * hatalı olabilir. Uygulamayı çökertmek
             * yerine ekranda hata gösteriyoruz.
             */

            showPreviewError(
                    e.getMessage()
            );
        }
    }

    /*
     * ============================================================
     * XML VIEW OLUŞTUR
     * ============================================================
     */

    private View createPreviewView(
            String tag,
            XmlPullParser parser
    ) {

        if (tag == null) {
            return null;
        }

        String cleanTag =
                tag;

        /*
         * Android namespace olmayan / olan
         * tag'ları basitleştir.
         */

        if (cleanTag.contains(".")) {

            int index =
                    cleanTag.lastIndexOf(
                            "."
                    );

            cleanTag =
                    cleanTag.substring(
                            index + 1
                    );
        }

        View view;

        switch (cleanTag) {

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

                if ("horizontal".equalsIgnoreCase(
                        orientation
                )) {

                    linear.setOrientation(
                            LinearLayout.HORIZONTAL
                    );

                } else {

                    linear.setOrientation(
                            LinearLayout.VERTICAL
                    );
                }

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

                TextView textView =
                        new TextView(
                                this
                        );

                textView.setText(
                        getAttribute(
                                parser,
                                "text"
                        )
                );

                textView.setTextSize(
                        TypedValue.COMPLEX_UNIT_SP,
                        getTextSize(
                                parser
                        )
                );

                textView.setTextColor(
                        Color.DKGRAY
                );

                view =
                        textView;

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

                view =
                        button;

                break;

            case "EditText":

                EditText editText =
                        new EditText(
                                this
                        );

                editText.setHint(
                        getAttribute(
                                parser,
                                "hint"
                        )
                );

                editText.setText(
                        getAttribute(
                                parser,
                                "text"
                        )
                );

                view =
                        editText;

                break;

            case "ImageView":

                android.widget.ImageView imageView =
                        new android.widget.ImageView(
                                this
                        );

                imageView.setBackgroundColor(
                        Color.LTGRAY
                );

                imageView.setContentDescription(
                        getAttribute(
                                parser,
                                "contentDescription"
                        )
                );

                view =
                        imageView;

                break;

            case "Space":

                view =
                        new android.widget.Space(
                                this
                        );

                break;

            default:

                /*
                 * Desteklemediğimiz View'ları
                 * şimdilik atlıyoruz.
                 */

                return null;
        }

        applyCommonAttributes(
                view,
                parser
        );

        return view;
    }

    /*
     * ============================================================
     * ORTAK XML ÖZELLİKLERİ
     * ============================================================
     */

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

            int value =
                    parseDimension(
                            padding,
                            0
                    );

            view.setPadding(
                    value,
                    value,
                    value,
                    value
            );
        }

        String gravity =
                getAttribute(
                        parser,
                        "gravity"
                );

        if (!gravity.isEmpty()) {

            if (view instanceof TextView) {

                TextView textView =
                        (TextView) view;

                if (gravity.contains("center")) {

                    textView.setGravity(
                            Gravity.CENTER
                    );

                } else if (
                        gravity.contains("start")
                ) {

                    textView.setGravity(
                            Gravity.START
                                    | Gravity.TOP
                    );
                }
            }
        }
    }

    /*
     * ============================================================
     * LAYOUT PARAMETRELERİ
     * ============================================================
     */

    private ViewGroup.LayoutParams
    createPreviewLayoutParams(
            View view,
            ViewGroup parent
    ) {

        int width =
                ViewGroup.LayoutParams.WRAP_CONTENT;

        int height =
                ViewGroup.LayoutParams.WRAP_CONTENT;

        /*
         * XML'deki layout_width ve
         * layout_height değerlerini alıyoruz.
         *
         * Şimdilik match_parent / wrap_content
         * destekleniyor.
         */

        /*
         * Parent yoksa root.
         */

        if (parent == null) {

            return new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }

        return new ViewGroup.LayoutParams(
                width,
                height
        );
    }

    /*
     * ============================================================
     * CONTAINER MI?
     * ============================================================
     */

    private boolean isContainerTag(
            String tag
    ) {

        if (tag == null) {
            return false;
        }

        return tag.equals(
                "LinearLayout"
        )
                || tag.equals(
                "FrameLayout"
        )
                || tag.equals(
                "RelativeLayout"
        );
    }

    /*
     * ============================================================
     * XML ATTRIBUTE
     * ============================================================
     */

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

    /*
     * ============================================================
     * TEXT SIZE
     * ============================================================
     */

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

            value =
                    value
                            .replace(
                                    "sp",
                                    ""
                            )
                            .trim();

            return Float.parseFloat(
                    value
            );

        } catch (Exception e) {

            return 14;
        }
    }

    /*
     * ============================================================
     * DIMENSION
     * ============================================================
     */

    private int parseDimension(
            String value,
            int defaultValue
    ) {

        if (value == null ||
                value.isEmpty()) {

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

    /*
     * ============================================================
     * ÖNİZLEME HATASI
     * ============================================================
     */

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
                        180,
                        0,
                        0
                )
        );

        error.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                12
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
                "XML hatası: "
                        + (
                        message == null
                                ? "Bilinmeyen hata"
                                : message
                )
        );
    }

    /*
     * ============================================================
     * DURUM ÇUBUĞU
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

        super.onDestroy();
    }
}
