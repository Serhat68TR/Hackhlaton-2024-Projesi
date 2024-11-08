package com.example.hackathon_ticaret_mektebi;

import android.content.Intent;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.ParcelFileDescriptor;

import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.bumptech.glide.Glide;
import com.example.hackathon_ticaret_mektebi.Models.Content;
import com.example.hackathon_ticaret_mektebi.Models.Student;
import com.example.hackathon_ticaret_mektebi.Models.Teacher;
import com.example.hackathon_ticaret_mektebi.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class YeniIcerikPaylasActivity extends AppCompatActivity {
    private static final int PICK_PDF_REQUEST = 1;
    Uri pdfUri;
    int pdfPageCount;
    ImageView yeni_icerik_pp;
    EditText contentNameEditText;
    TextView contentProviderTextView;
    EditText contentSharedDateEditText;
    EditText contentDepartmentEditText;
    TextView contentSizeEditText;
    DatabaseReference contentDatabaseRef;
    TextView yeni_icerik_ad_soyad_text;
    String userName;
    String userDepartment;
    String userPhotoUrl;
    FirebaseUser user;
    String userID;
    ImageButton gemini;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.yeni_icerik_paylas_activity);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.yeni_icerik_l), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        gemini=findViewById(R.id.yeni_icerik_yapay_zeka);
        gemini.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(YeniIcerikPaylasActivity.this, GeminiActivity.class);
                startActivity(intent);
            }
        });
        yeni_icerik_pp = findViewById(R.id.yeni_icerik_pp);
        getUserInfoFromDatabase();
        yeni_icerik_pp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                user = getCurrentUser();
                if (user != null) {
                    String userID = user.getUid();
                    DatabaseReference teacherRef = FirebaseDatabase.getInstance().getReference("teachers").child(userID);
                    teacherRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                // Eğer kullanıcı teacher ise OgretmenArayuzActivity'i aç
                                Intent intent = new Intent(YeniIcerikPaylasActivity.this, OgretmenArayuzActivity.class);
                                startActivity(intent);
                            } else {
                                // Eğer kullanıcı teacher değilse, student'ı kontrol et
                                DatabaseReference studentRef = FirebaseDatabase.getInstance().getReference("students").child(userID);
                                studentRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot studentSnapshot) {
                                        if (studentSnapshot.exists()) {
                                            // Eğer kullanıcı student ise OgrenciArayuzActivity'i aç
                                            Intent intent = new Intent(YeniIcerikPaylasActivity.this, OgrenciArayuzActivity.class);
                                            startActivity(intent);
                                            finish(); // Kullanıcı geri basarsa login ekranına dönmesin diye
                                        } else {
                                            // Kullanıcı ne öğretmen ne de öğrenci
                                            Log.d("UserInfo", "Kullanıcı ne öğretmen ne de öğrenci.");
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        Log.e("UserInfo", "Error: " + databaseError.getMessage());
                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.e("UserInfo", "Error: " + databaseError.getMessage());
                        }
                    });
                } else {
                    Log.d("UserInfo", "No user is signed in.");
                }
            }
        });
        contentNameEditText = findViewById(R.id.yeni_icerik_icerigin_adi_dt);
        contentProviderTextView = findViewById(R.id.yeni_icerik_paylasan_kisi_dt);
        contentSharedDateEditText = findViewById(R.id.yeni_icerik_paylasilan_tarih_dt);
        contentDepartmentEditText = findViewById(R.id.yeni_icerik_icerik_bolum_adi_dt);
        contentSizeEditText = findViewById(R.id.yeni_icerik_icerik_sayfa_boyut_dt);

        // Tarihi otomatik olarak al ve alanı doldur
        String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        contentSharedDateEditText.setText(currentDate);

        yeni_icerik_ad_soyad_text = findViewById(R.id.yeni_icerik_ad_soyad_text);
        Button selectFileButton = findViewById(R.id.yeni_icerik_paylas_dosyaSec);
        Button shareContentButton = findViewById(R.id.yeni_icerik_paylas_btn);

        contentDatabaseRef = FirebaseDatabase.getInstance().getReference("contents");

        selectFileButton.setOnClickListener(v -> openFileChooser());
        shareContentButton.setOnClickListener(v -> uploadPdfToFirebase());


    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("application/pdf");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "PDF Seç"), PICK_PDF_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PDF_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            pdfUri = data.getData();

            // PDF sayfa sayısını al
            try {
                pdfPageCount = getPdfPageCount(pdfUri);
                contentSizeEditText.setText(String.valueOf(pdfPageCount) + " Sayfa");
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "PDF sayfa sayısı alınamadı.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private int getPdfPageCount(Uri pdfUri) throws IOException {
        ParcelFileDescriptor fileDescriptor = getContentResolver().openFileDescriptor(pdfUri, "r");
        PdfRenderer pdfRenderer = new PdfRenderer(fileDescriptor);
        int pageCount = pdfRenderer.getPageCount();
        pdfRenderer.close();
        fileDescriptor.close();
        return pageCount;
    }

    private void uploadPdfToFirebase() {
        if (pdfUri != null) {
            StorageReference storageRef = FirebaseStorage.getInstance().getReference("contents").child(System.currentTimeMillis() + ".pdf");

            storageRef.putFile(pdfUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String fileUrl = uri.toString();
                            saveContentDataToDatabase(fileUrl);  // Veritabanına kaydet
                        });
                    })
                    .addOnFailureListener(e -> Toast.makeText(YeniIcerikPaylasActivity.this, "PDF yüklenemedi.", Toast.LENGTH_SHORT).show());
        }
    }

    private void saveContentDataToDatabase(String fileUrl) {
        String contentName = contentNameEditText.getText().toString().trim();
        String contentProvider = contentProviderTextView.getText().toString().trim();
        String contentSharedDate = contentSharedDateEditText.getText().toString().trim();
        String contentDepartment = contentDepartmentEditText.getText().toString().trim();
        String contentSize = contentSizeEditText.getText().toString().trim();

        if (!contentName.isEmpty() && !contentProvider.isEmpty() && !contentSharedDate.isEmpty() && !contentDepartment.isEmpty() && !contentSize.isEmpty()) {
            String contentId = contentDatabaseRef.push().getKey();
            Content content = new Content(contentDepartment, contentName, contentProvider, contentSharedDate, contentSize, fileUrl);
            contentDatabaseRef.child(contentId).setValue(content)
                    .addOnSuccessListener(aVoid -> Toast.makeText(YeniIcerikPaylasActivity.this, "İçerik başarıyla paylaşıldı.", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(YeniIcerikPaylasActivity.this, "İçerik paylaşılırken hata oluştu.", Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "Tüm alanlar doldurulmalıdır.", Toast.LENGTH_SHORT).show();
        }
    }

    // Kullanıcı bilgilerini Firebase Realtime Database'den çeker

    public void getUserInfoFromDatabase() {
        user = getCurrentUser();
        if (user != null) {
            userID = user.getUid();

            // Önce teachers düğümünde kontrol edelim
            DatabaseReference teacherRef = FirebaseDatabase.getInstance().getReference("teachers").child(userID);
            teacherRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // Kullanıcı teacher düğümündeyse
                        Teacher teacher = dataSnapshot.getValue(Teacher.class);
                        if (teacher != null) {

                            userName = teacher.getNameSurname();
                            userDepartment = teacher.getUserDepartment();
                            userPhotoUrl = teacher.getProfilePictureURL();

                            contentProviderTextView.setText(userName);
                            if (userPhotoUrl != null) {
                                Glide.with(YeniIcerikPaylasActivity.this).load(userPhotoUrl).into(yeni_icerik_pp);
                            } else {
                                Log.e("ImageLoadError", "Profil resmi URL'si null."); // URL'nin null olup olmadığını kontrol et
                            }
                            yeni_icerik_ad_soyad_text.setText(userName);
                            Log.d("UserInfo", "Teacher - User ID: " + userID + ", Name: " + userName + ", Department: " + userDepartment + ", Photo URL: " + userPhotoUrl);
                        }
                    } else {
                        // Eğer teacher düğümünde değilse, students düğümünü kontrol et
                        DatabaseReference studentRef = FirebaseDatabase.getInstance().getReference("students").child(userID);
                        studentRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot studentSnapshot) {
                                if (studentSnapshot.exists()) {
                                    // Kullanıcı student düğümündeyse
                                    Student student = studentSnapshot.getValue(Student.class);
                                    if (student != null) {
                                        userName = student.getNameSurname();
                                        userDepartment = student.getUserDepartment();
                                        userPhotoUrl = student.getProfilePictureURL();
                                        if (userPhotoUrl != null) {
                                            Glide.with(YeniIcerikPaylasActivity.this).load(userPhotoUrl).into(yeni_icerik_pp);
                                        } else {
                                            Log.e("ImageLoadError", "Profil resmi URL'si null."); // URL'nin null olup olmadığını kontrol et
                                        }
                                        yeni_icerik_ad_soyad_text.setText(userName);
                                        Log.d("UserInfo", "Student - User ID: " + userID + ", Name: " + userName + ", Class: " + ", Photo URL: " + userPhotoUrl);
                                    }
                                } else {
                                    Log.d("UserInfo", "Kullanıcı ne öğretmen ne de öğrenci.");
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Log.e("UserInfo", "Error: " + databaseError.getMessage());
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e("UserInfo", "Error: " + databaseError.getMessage());
                }
            });
        } else {
            Log.d("UserInfo", "No user is signed in.");
        }
    }
    // Firebase'den giriş yapmış kullanıcıyı döndürür
    public FirebaseUser getCurrentUser() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        return mAuth.getCurrentUser();
    }

}
