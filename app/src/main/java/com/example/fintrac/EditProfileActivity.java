package com.example.fintrac;

import android.app.DatePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends BaseActivity {

    private ImageView btnBack, ivProfilePicture;
    private MaterialCardView cardProfileImage;
    private TextView tvChangePhoto;
    private EditText etFullName, etEmail, etBirthday, etOccupation;
    private Spinner spinnerGender;
    private MaterialButton btnSaveProfile;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userID;

    private Uri selectedImageUri = null;
    private String selectedAvatarName = null;

    private final ActivityResultLauncher<String> pickMedia =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    selectedAvatarName = null;
                    loadProfileImage(uri, null);
                    Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SettingHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userID = currentUser.getUid();
        } else {
            userID = "guest_user";
        }

        initViews();
        setupGenderSpinner();
        loadExistingProfile();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        cardProfileImage = findViewById(R.id.cardProfileImage);
        tvChangePhoto = findViewById(R.id.tvChangePhoto);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etBirthday = findViewById(R.id.etBirthday);
        etOccupation = findViewById(R.id.etOccupation);
        spinnerGender = findViewById(R.id.spinnerGender);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);

        etEmail.setEnabled(false);
        etEmail.setFocusable(false);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        cardProfileImage.setOnClickListener(v -> showAvatarMenu());
        tvChangePhoto.setOnClickListener(v -> showAvatarMenu());
        etBirthday.setOnClickListener(v -> showDatePicker());
        btnSaveProfile.setOnClickListener(v -> validateAndSaveProfile());
    }

    private void loadProfileImage(Uri uri, Integer resourceId) {
        try {
            if (uri != null) {
                Glide.with(this)
                        .load(uri)
                        .circleCrop()
                        .placeholder(ContextCompat.getDrawable(this, R.drawable.ic_person))
                        .error(ContextCompat.getDrawable(this, R.drawable.ic_person))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(ivProfilePicture);
                Log.d("EditProfile", "Loaded image from URI");
            } else if (resourceId != null && resourceId != 0) {
                Glide.with(this)
                        .load(resourceId)
                        .circleCrop()
                        .placeholder(ContextCompat.getDrawable(this, R.drawable.ic_person))
                        .error(ContextCompat.getDrawable(this, R.drawable.ic_person))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(ivProfilePicture);
                Log.d("EditProfile", "Loaded image from resource: " + resourceId);
            }
        } catch (Exception e) {
            Log.e("EditProfile", "Error loading image", e);
            ivProfilePicture.setImageResource(R.drawable.ic_person);
        }
    }

    private void showAvatarMenu() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_choose_avatar, null, false);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setDimAmount(0.6f);
        }

        MaterialButton btnGallery = view.findViewById(R.id.btnChooseGallery);

        // Find all avatar containers and set click listeners
        int[] containerIds = {
                R.id.avatar1_container, R.id.avatar2_container, R.id.avatar3_container, R.id.avatar4_container,
                R.id.avatar5_container, R.id.avatar6_container, R.id.avatar7_container, R.id.avatar8_container,
                R.id.avatar9_container, R.id.avatar10_container, R.id.avatar11_container, R.id.avatar12_container,
                R.id.avatar13_container, R.id.avatar14_container, R.id.avatar15_container, R.id.avatar16_container,
                R.id.avatar18_container, R.id.avatar19_container, R.id.avatar20_container
        };

        int[] drawableIds = {
                R.drawable.avatar_1, R.drawable.avatar_2, R.drawable.avatar_3, R.drawable.avatar_4,
                R.drawable.avatar_5, R.drawable.avatar_6, R.drawable.avatar_7, R.drawable.avatar_8,
                R.drawable.avatar_9, R.drawable.avatar_10, R.drawable.avatar_11, R.drawable.avatar_12,
                R.drawable.avatar_13, R.drawable.avatar_14, R.drawable.avatar_15, R.drawable.avatar_16,
                R.drawable.avatar_18, R.drawable.avatar_19, R.drawable.avatar_20
        };

        String[] avatarNames = {
                "avatar_1", "avatar_2", "avatar_3", "avatar_4",
                "avatar_5", "avatar_6", "avatar_7", "avatar_8",
                "avatar_9", "avatar_10", "avatar_11", "avatar_12",
                "avatar_13", "avatar_14", "avatar_15", "avatar_16",
                "avatar_18", "avatar_19", "avatar_20"
        };

        for (int i = 0; i < containerIds.length; i++) {
            final int index = i;
            View container = view.findViewById(containerIds[i]);
            if (container != null) {
                // Find the ImageView inside the container to preload the avatar
                int imageId = 0;
                if (index < 16) {
                    imageId = getResources().getIdentifier("avatar" + (index + 1), "id", getPackageName());
                } else {
                    imageId = getResources().getIdentifier("avatar" + (index + 2), "id", getPackageName());
                }

                ImageView avatarImage = view.findViewById(imageId);
                if (avatarImage != null) {
                    Glide.with(this)
                            .load(drawableIds[index])
                            .circleCrop()
                            .into(avatarImage);
                }

                container.setOnClickListener(v -> {
                    selectedAvatarName = avatarNames[index];
                    selectedImageUri = null;
                    loadProfileImage(null, drawableIds[index]);
                    Toast.makeText(this, "Avatar selected", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            }
        }

        btnGallery.setOnClickListener(v -> {
            dialog.dismiss();
            pickMedia.launch("image/*");
        });

        dialog.show();
    }

    private void setupGenderSpinner() {
        String[] genders = {"Select Gender", "Male", "Female", "Non-Binary", "Prefer not to say"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, genders);
        spinnerGender.setAdapter(adapter);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = (selectedMonth + 1) + "/" + selectedDay + "/" + selectedYear;
                    etBirthday.setText(formattedDate);
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    @SuppressWarnings("unchecked")
    private void loadExistingProfile() {
        if (userID.equals("guest_user")) return;

        db.collection("users").document(userID).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("fullName");
                        if (fullName == null || fullName.isEmpty()) {
                            FirebaseUser currentUser = mAuth.getCurrentUser();
                            if (currentUser != null && currentUser.getDisplayName() != null) {
                                fullName = currentUser.getDisplayName();
                            }
                        }
                        etFullName.setText(fullName != null ? fullName : "");

                        String email = documentSnapshot.getString("email");
                        if (email == null || email.isEmpty()) {
                            FirebaseUser currentUser = mAuth.getCurrentUser();
                            if (currentUser != null) {
                                email = currentUser.getEmail();
                            }
                        }
                        etEmail.setText(email != null ? email : "");

                        etBirthday.setText(documentSnapshot.getString("birthday") != null ?
                                documentSnapshot.getString("birthday") : "");
                        etOccupation.setText(documentSnapshot.getString("occupation") != null ?
                                documentSnapshot.getString("occupation") : "");

                        String gender = documentSnapshot.getString("gender");
                        if (gender != null && spinnerGender.getAdapter() != null) {
                            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerGender.getAdapter();
                            int position = adapter.getPosition(gender);
                            if (position >= 0) {
                                spinnerGender.setSelection(position);
                            }
                        }

                        String savedAvatar = documentSnapshot.getString("profileAvatar");
                        String savedUri = documentSnapshot.getString("profileImageLocalUri");

                        if (savedUri != null && !savedUri.isEmpty()) {
                            selectedImageUri = Uri.parse(savedUri);
                            loadProfileImage(selectedImageUri, null);
                        } else if (savedAvatar != null && !savedAvatar.isEmpty()) {
                            selectedAvatarName = savedAvatar;
                            int resId = getResources().getIdentifier(savedAvatar, "drawable", getPackageName());
                            if (resId != 0) {
                                loadProfileImage(null, resId);
                            } else {
                                loadProfileImage(null, R.drawable.ic_person);
                            }
                        } else {
                            loadProfileImage(null, R.drawable.ic_person);
                        }
                    } else {
                        FirebaseUser currentUser = mAuth.getCurrentUser();
                        if (currentUser != null) {
                            if (currentUser.getDisplayName() != null) {
                                etFullName.setText(currentUser.getDisplayName());
                            }
                            etEmail.setText(currentUser.getEmail());
                            createInitialUserDocument(currentUser);
                            loadProfileImage(null, R.drawable.ic_person);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                    Log.e("EditProfile", "Error loading profile", e);
                    loadProfileImage(null, R.drawable.ic_person);
                });
    }

    private void createInitialUserDocument(FirebaseUser user) {
        Map<String, Object> initialData = new HashMap<>();
        initialData.put("fullName", user.getDisplayName());
        initialData.put("email", user.getEmail());
        initialData.put("createdAt", FieldValue.serverTimestamp());

        db.collection("users").document(user.getUid())
                .set(initialData, SetOptions.merge())
                .addOnFailureListener(e ->
                        Log.e("EditProfile", "Failed to create initial document", e));
    }

    private void validateAndSaveProfile() {
        if (userID.equals("guest_user")) {
            Toast.makeText(this, "Cannot save. Please login first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = etFullName.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        performSaveToFirestore(name);
    }

    private void performSaveToFirestore(String name) {
        btnSaveProfile.setEnabled(false);
        btnSaveProfile.setText("Saving...");

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("fullName", name);
        profileData.put("birthday", etBirthday.getText().toString().trim());
        profileData.put("occupation", etOccupation.getText().toString().trim());
        profileData.put("email", etEmail.getText().toString().trim());

        String gender = spinnerGender.getSelectedItem().toString();
        if (!gender.equals("Select Gender")) {
            profileData.put("gender", gender);
        }

        if (selectedImageUri != null) {
            profileData.put("profileImageLocalUri", selectedImageUri.toString());
            profileData.put("profileAvatar", FieldValue.delete());
        } else if (selectedAvatarName != null) {
            profileData.put("profileAvatar", selectedAvatarName);
            profileData.put("profileImageLocalUri", FieldValue.delete());
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build();
            user.updateProfile(profileUpdates)
                    .addOnFailureListener(e ->
                            Log.e("EditProfile", "Failed to update auth profile", e));
        }

        db.collection("users").document(userID)
                .set(profileData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile Saved Successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnSaveProfile.setEnabled(true);
                    btnSaveProfile.setText("Save Changes");
                    Log.e("EditProfile", "Firestore save error", e);
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadExistingProfile();
    }
}