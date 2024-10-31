package com.example.hackhaton_ticaret_mektebi;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.hackhaton_ticaret_mektebi.Models.Teacher;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.UUID;
import java.util.UUID; // UUID sınıfını içeri aktarın
public class OgretmenArayuzActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;
    private ImageView userProfilePictureImageView, ogretmen_profil_foto;
    private DatabaseReference userDatabaseRef;
    String userID;
    String userName;
    FirebaseUser user;
    // FirebaseAuth nesnesi oluşturuluyor
    FirebaseAuth mAuth = FirebaseAuth.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ogretmen_arayuz_activity);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.ogretmen_arayuz), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        getUserInfoFromDatabase();

        userProfilePictureImageView = findViewById(R.id.ogretmen_arayuz_profil);
        Button changeProfilePicButton = findViewById(R.id.ogr_pp_degistir_btn);
        ogretmen_profil_foto = findViewById(R.id.ogretmen_profil_foto);

        changeProfilePicButton.setOnClickListener(v -> openGallery());

    }




    public void getUserInfoFromDatabase() {
        user = getCurrentUser();
        if (user != null) {
            userID = user.getUid();
            Log.d("Deneme0", userID);
            // Veritabanından Teacher bilgilerini çekmek için
            userDatabaseRef = FirebaseDatabase.getInstance().getReference("teachers").child(userID);
            userDatabaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Teacher teacher = dataSnapshot.getValue(Teacher.class);
                    if (teacher != null) {
                        loadUserProfilePicture();  // Mevcut profil fotoğrafını yükle
                        userName = teacher.getNameSurname();

                        Log.d("Deneme1", userID);
                        Log.d("UserInfo", "User ID: " + userID + ", Name: " + userName);
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

    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Resim Seç"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                userProfilePictureImageView.setImageBitmap(bitmap);
                ogretmen_profil_foto.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }

            uploadImageToFirebase();
        }
    }

    private void uploadImageToFirebase() {
        if (userID != null) {
            FirebaseStorage storage = FirebaseStorage.getInstance();

            // Rastgele bir UUID oluştur
            String uniqueID = UUID.randomUUID().toString(); // Rastgele UUID
            String fileName = "profilePictures/" + uniqueID + ".jpg"; // Rastgele isim ile dosya adı

            StorageReference storageRef = storage.getReference().child(fileName);

            UploadTask uploadTask = storageRef.putFile(imageUri);
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String profilePictureURL = uri.toString();
                    Log.d("FirebaseURL", "Profil resmi URL'si: " + profilePictureURL); // URL'yi logla
                    updateProfilePictureInDatabase(userID, profilePictureURL);
                });
            }).addOnFailureListener(e -> {
                Log.e("FirebaseError", "Resim yüklenirken hata oluştu: " + e.getMessage()); // Hata mesajını logla
                Toast.makeText(OgretmenArayuzActivity.this, "Resim yüklenirken hata oluştu.", Toast.LENGTH_SHORT).show();
            });
        } else {
            Log.e("FirebaseAuth", "Kullanıcı oturumu kapalı."); // Kullanıcı oturumunun açık olup olmadığını kontrol et
        }
    }

    private void updateProfilePictureInDatabase(String userId, String profilePictureURL) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("teachers").child(userId);
        dbRef.child("profilePictureURL").setValue(profilePictureURL)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(OgretmenArayuzActivity.this, "Profil resmi başarıyla güncellendi!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(OgretmenArayuzActivity.this, "Profil resmi güncellenirken hata oluştu.", Toast.LENGTH_SHORT).show();
                });
        Log.d("ProfilePictureURL", "URL: " + profilePictureURL);
    }

    private void loadUserProfilePicture() {
        userDatabaseRef.child("profilePictureURL").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String profilePictureURL = snapshot.getValue(String.class);
                Log.d("ProfilePictureURL", "Yüklenmeye çalışılan URL: " + profilePictureURL); // URL'yi logla
                if (profilePictureURL != null) {
                    Glide.with(OgretmenArayuzActivity.this).load(profilePictureURL).into(userProfilePictureImageView);
                    Glide.with(OgretmenArayuzActivity.this).load(profilePictureURL).into(ogretmen_profil_foto);
                } else {
                    Log.e("ImageLoadError", "Profil resmi URL'si null."); // URL'nin null olup olmadığını kontrol et
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("DatabaseError", "Profil resmi yüklenemedi: " + error.getMessage()); // Hata mesajını logla
            }
        });
    }

    // Firebase'den giriş yapmış kullanıcıyı döndürür
    public FirebaseUser getCurrentUser() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        return mAuth.getCurrentUser();
    }


}
