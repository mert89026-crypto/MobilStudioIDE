package com.mobilstudio.ide;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private File rootProjectDir;

    private final ArrayList<String> projectList =
            new ArrayList<>();

    private final ArrayList<String> filteredList =
            new ArrayList<>();

    private ArrayAdapter<String> adapter;

    private ListView listView;
    private EditText etSearch;

    private Button btnNewProject;
    private FloatingActionButton fabNewProject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_dashboard
        );

        /*
         * PROJE ANA KLASÖRÜ
         */

        rootProjectDir = new File(
                getExternalFilesDir(null),
                "MobilStudio_Projects"
        );

        if (!rootProjectDir.exists()) {

            boolean created =
                    rootProjectDir.mkdirs();

            if (!created
                    && !rootProjectDir.exists()) {

                Toast.makeText(
                        this,
                        "Proje klasörü oluşturulamadı",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }
        }

        /*
         * XML ELEMANLARI
         */

        listView = findViewById(
                R.id.listProjects
        );

        etSearch = findViewById(
                R.id.etSearch
        );

        btnNewProject = findViewById(
                R.id.btnNewProject
        );

        fabNewProject = findViewById(
                R.id.fabNewProject
        );

        /*
         * PROJE LİSTESİ
         */

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                filteredList
        );

        listView.setAdapter(adapter);

        /*
         * PROJELERİ YÜKLE
         */

        loadProjects();

        /*
         * YENİ PROJE BUTONU
         */

        btnNewProject.setOnClickListener(
                v -> showNewProjectDialog()
        );

        /*
         * FAB
         */

        fabNewProject.setOnClickListener(
                v -> showNewProjectDialog()
        );

        /*
         * PROJEYE TIKLAMA
         */

        listView.setOnItemClickListener(
                (parent, view, position, id) -> {

                    if (position < 0
                            || position >= filteredList.size()) {

                        return;
                    }

                    String projectName =
                            filteredList.get(position);

                    File repo =
                            new File(
                                    rootProjectDir,
                                    projectName
                            );

                    if (!repo.exists()
                            || !repo.isDirectory()) {

                        Toast.makeText(
                                DashboardActivity.this,
                                "Proje bulunamadı",
                                Toast.LENGTH_SHORT
                        ).show();

                        loadProjects();

                        return;
                    }

                    Intent intent =
                            new Intent(
                                    DashboardActivity.this,
                                    EditorActivity.class
                            );

                    intent.putExtra(
                            "REPO_PATH",
                            repo.getAbsolutePath()
                    );

                    startActivity(intent);
                }
        );

        /*
         * ARAMA
         */

        etSearch.addTextChangedListener(
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

                        filterProjects(
                                s == null
                                        ? ""
                                        : s.toString()
                        );
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }
                }
        );
    }

    @Override
    protected void onResume() {

        super.onResume();

        loadProjects();
    }

    /*
     * PROJELERİ OKU
     */

    private void loadProjects() {

        projectList.clear();

        if (rootProjectDir == null) {
            return;
        }

        if (!rootProjectDir.exists()) {

            if (!rootProjectDir.mkdirs()) {

                Toast.makeText(
                        this,
                        "Proje klasörü oluşturulamadı",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }
        }

        File[] files =
                rootProjectDir.listFiles();

        if (files != null) {

            for (File file : files) {

                if (file != null
                        && file.isDirectory()
                        && !file.isHidden()) {

                    projectList.add(
                            file.getName()
                    );
                }
            }
        }

        Collections.sort(
                projectList,
                String.CASE_INSENSITIVE_ORDER
        );

        String searchText = "";

        if (etSearch != null) {

            searchText =
                    etSearch
                            .getText()
                            .toString();
        }

        filterProjects(searchText);
    }

    /*
     * PROJE ARAMA
     */

    private void filterProjects(
            String text
    ) {

        filteredList.clear();

        String search =
                text == null
                        ? ""
                        : text.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        for (String project :
                projectList) {

            if (project == null) {
                continue;
            }

            if (search.isEmpty()
                    || project
                    .toLowerCase(
                            Locale.ROOT
                    )
                    .contains(search)) {

                filteredList.add(
                        project
                );
            }
        }

        if (adapter != null) {

            adapter.notifyDataSetChanged();
        }
    }

    /*
     * YENİ PROJE PENCERESİ
     */

    private void showNewProjectDialog() {

        final EditText input =
                new EditText(this);

        input.setSingleLine(true);

        input.setHint(
                "Proje adı"
        );

        input.setPadding(
                dp(16),
                dp(8),
                dp(16),
                dp(8)
        );

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "Yeni Proje"
                        )
                        .setView(input)
                        .setPositiveButton(
                                "Oluştur",
                                null
                        )
                        .setNegativeButton(
                                "İptal",
                                null
                        )
                        .create();

        dialog.setOnShowListener(
                d -> {

                    Button createButton =
                            dialog.getButton(
                                    AlertDialog.BUTTON_POSITIVE
                            );

                    createButton.setOnClickListener(
                            v -> {

                                String projectName =
                                        input.getText()
                                                .toString()
                                                .trim();

                                if (projectName.isEmpty()) {

                                    input.setError(
                                            "Proje adı boş olamaz"
                                    );

                                    return;
                                }

                                if (!isValidProjectName(
                                        projectName
                                )) {

                                    input.setError(
                                            "Geçersiz proje adı"
                                    );

                                    Toast.makeText(
                                            this,
                                            "Sadece geçerli bir klasör adı kullan",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    return;
                                }

                                createProject(
                                        projectName,
                                        dialog
                                );
                            }
                    );

                    input.requestFocus();
                }
        );

        dialog.getWindow();

        dialog.show();
    }

    /*
     * PROJE OLUŞTUR
     */

    private void createProject(
            String projectName,
            AlertDialog dialog
    ) {

        File repo =
                new File(
                        rootProjectDir,
                        projectName
                );

        /*
         * ANA KLASÖR DIŞINA ÇIKILMASINI ENGELLE
         */

        try {

            String rootPath =
                    rootProjectDir
                            .getCanonicalPath();

            String repoPath =
                    repo
                            .getCanonicalPath();

            if (!repoPath.equals(rootPath)
                    && !repoPath.startsWith(
                    rootPath
                            + File.separator
            )) {

                Toast.makeText(
                        this,
                        "Geçersiz proje yolu",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Proje yolu kontrol edilemedi",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        /*
         * AYNI PROJE VAR MI?
         */

        if (repo.exists()) {

            Toast.makeText(
                    this,
                    "Bu proje zaten var",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        /*
         * PROJE KLASÖRÜ
         */

        boolean created =
                repo.mkdirs();

        if (!created
                && !repo.exists()) {

            Toast.makeText(
                    this,
                    "Proje oluşturulamadı",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        /*
         * BAŞLANGIÇ KLASÖRLERİ
         *
         * Şimdilik boş Android projesi
         * iskeleti hazırlıyoruz.
         */

        createDirectory(
                repo,
                "app"
        );

        createDirectory(
                repo,
                "app/src"
        );

        createDirectory(
                repo,
                "app/src/main"
        );

        createDirectory(
                repo,
                "app/src/main/java"
        );

        createDirectory(
                repo,
                "app/src/main/res"
        );

        createDirectory(
                repo,
                "app/src/main/res/layout"
        );

        createDirectory(
                repo,
                "app/src/main/res/drawable"
        );

        createDirectory(
                repo,
                "app/src/main/res/mipmap"
        );

        createDirectory(
                repo,
                "app/src/main/res/values"
        );

        /*
         * LİSTEYİ YENİLE
         */

        loadProjects();

        /*
         * PENCEREYİ KAPAT
         */

        if (dialog != null
                && dialog.isShowing()) {

            dialog.dismiss();
        }

        Toast.makeText(
                this,
                "Proje oluşturuldu",
                Toast.LENGTH_SHORT
        ).show();
    }

    /*
     * KLASÖR OLUŞTUR
     */

    private boolean createDirectory(
            File parent,
            String name
    ) {

        File directory =
                new File(
                        parent,
                        name
                );

        if (directory.exists()) {

            return directory.isDirectory();
        }

        return directory.mkdirs();
    }

    /*
     * PROJE ADI KONTROLÜ
     */

    private boolean isValidProjectName(
            String name
    ) {

        if (name == null
                || name.isEmpty()) {

            return false;
        }

        if (name.equals(".")
                || name.equals("..")) {

            return false;
        }

        /*
         * Windows / Linux / Android
         * için sorun çıkarabilecek
         * karakterleri engelliyoruz.
         */

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

    /*
     * DP
     */

    private int dp(int value) {

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        return (int) (
                value * density
                        + 0.5f
        );
    }
}
