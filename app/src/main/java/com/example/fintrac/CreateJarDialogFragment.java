package com.example.fintrac;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CreateJarDialogFragment extends DialogFragment {

    public static final String REQUEST_KEY = "create_jar_request";
    public static final String KEY_JAR_NAME = "jar_name";
    public static final String KEY_TARGET_AMOUNT = "target_amount";
    public static final String KEY_JAR_TYPE = "jar_type";
    public static final String KEY_SELECTED_ICON = "selected_icon";
    public static final String KEY_SELECTED_COLOR = "selected_color";
    public static final String KEY_DEADLINE = "deadline";

    private EditText jarNameInput, targetAmountInput, deadlineInput;
    private ChipGroup jarTypeGroup;
    private ImageView selectedIcon = null;
    private ImageView selectedColorView = null;
    private String selectedIconName = "hat";
    private int selectedColorInt = Color.parseColor("#F44336"); // Default red
    private Button createButton;

    // Store the selected deadline in both formats
    private String selectedDeadlineDisplay = ""; // For showing to user (e.g., "Feb 28, 2026")
    private String selectedDeadlineStorage = ""; // For storing in Firestore (e.g., "2026-02-28")

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.ThemeOverlay_App_Dialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_create_jar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupIconSelection(view);
        setupColorSelection(view);
        setupDeadlinePicker();
        setupAmountFormatting();
        setupCreateButton(view);
        setupCloseButton(view);
    }

    private void initViews(View view) {
        jarNameInput = view.findViewById(R.id.jar_name_input);
        targetAmountInput = view.findViewById(R.id.target_amount_input);
        deadlineInput = view.findViewById(R.id.deadline_input);
        jarTypeGroup = view.findViewById(R.id.jar_type_group);
        createButton = view.findViewById(R.id.create_jar_button);

        Chip flexibleChip = view.findViewById(R.id.flexible_chip);
        if (flexibleChip != null) {
            flexibleChip.setChecked(true);
        }
    }

    private void setupIconSelection(View view) {
        View.OnClickListener iconClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedIcon != null) {
                    selectedIcon.setBackgroundResource(R.drawable.bg_icon_unselected);
                }

                v.setBackgroundResource(R.drawable.bg_icon_selected);
                selectedIcon = (ImageView) v;

                // FIXED: Map the view IDs to the correct icon names
                int viewId = v.getId();

                if (viewId == R.id.icon_hat) {
                    selectedIconName = "hat";
                } else if (viewId == R.id.icon_house) {
                    selectedIconName = "house";
                } else if (viewId == R.id.icon_plane) {
                    selectedIconName = "airplane"; // Map to airplane
                } else if (viewId == R.id.icon_laptop) {
                    selectedIconName = "laptop";
                } else if (viewId == R.id.icon_car) {
                    selectedIconName = "car";
                } else if (viewId == R.id.icon_game) {
                    selectedIconName = "controller"; // Map to controller
                } else if (viewId == R.id.icon_shirt) {
                    selectedIconName = "tshirt"; // Map to tshirt
                } else if (viewId == R.id.icon_book) {
                    selectedIconName = "book";
                } else if (viewId == R.id.icon_burger) {
                    selectedIconName = "burger";
                } else if (viewId == R.id.icon_gift) {
                    selectedIconName = "ic_gift"; // Map to ic_gift
                } else {
                    // Fallback to extracting from view ID (remove "icon_" prefix)
                    String viewIdName = getResources().getResourceEntryName(viewId);
                    selectedIconName = viewIdName.replace("icon_", "");
                }

                Log.d("CreateJarDialog", "Selected icon: " + selectedIconName);
            }
        };

        int[] iconIds = {
                R.id.icon_hat, R.id.icon_house, R.id.icon_plane, R.id.icon_laptop,
                R.id.icon_car, R.id.icon_game, R.id.icon_shirt, R.id.icon_book,
                R.id.icon_burger, R.id.icon_gift
        };

        for (int id : iconIds) {
            ImageView icon = view.findViewById(id);
            if (icon != null) {
                icon.setOnClickListener(iconClickListener);
            }
        }

        // Set default selected icon
        ImageView defaultIcon = view.findViewById(R.id.icon_hat);
        if (defaultIcon != null) {
            defaultIcon.setBackgroundResource(R.drawable.bg_icon_selected);
            selectedIcon = defaultIcon;
            selectedIconName = "hat";
        }
    }

    private void setupColorSelection(View view) {
        View.OnClickListener colorClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();

                // Check if custom color is clicked
                if (id == R.id.color_custom) {
                    openColorPickerDialog();
                    return;
                }

                // Handle regular color selection
                if (selectedColorView != null) {
                    selectedColorView.setBackgroundResource(R.drawable.bg_color_circle);
                }

                v.setBackgroundResource(R.drawable.bg_color_circle);
                v.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100).start();
                if (selectedColorView != null && selectedColorView != v) {
                    selectedColorView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                }

                selectedColorView = (ImageView) v;

                if (id == R.id.color_red) selectedColorInt = Color.parseColor("#F44336");
                else if (id == R.id.color_blue) selectedColorInt = Color.parseColor("#2196F3");
                else if (id == R.id.color_green) selectedColorInt = Color.parseColor("#4CAF50");
                else if (id == R.id.color_purple) selectedColorInt = Color.parseColor("#9C27B0");
                else if (id == R.id.color_orange) selectedColorInt = Color.parseColor("#FF9800");
                else if (id == R.id.color_teal) selectedColorInt = Color.parseColor("#009688");
                else if (id == R.id.color_pink) selectedColorInt = Color.parseColor("#E91E63");
                else if (id == R.id.color_indigo) selectedColorInt = Color.parseColor("#3F51B5");
                else if (id == R.id.color_black) selectedColorInt = Color.parseColor("#000000");

                Log.d("CreateJarDialog", "Selected color: " + selectedColorInt);
            }
        };

        int[] colorIds = {
                R.id.color_red, R.id.color_blue, R.id.color_green, R.id.color_purple,
                R.id.color_orange, R.id.color_teal, R.id.color_pink, R.id.color_indigo,
                R.id.color_black, R.id.color_custom
        };

        for (int id : colorIds) {
            ImageView color = view.findViewById(id);
            if (color != null) {
                color.setOnClickListener(colorClickListener);
            }
        }

        ImageView defaultColor = view.findViewById(R.id.color_red);
        if (defaultColor != null) {
            defaultColor.animate().scaleX(1.2f).scaleY(1.2f).setDuration(0).start();
            selectedColorView = defaultColor;
            selectedColorInt = Color.parseColor("#F44336");
        }
    }

    private void openColorPickerDialog() {
        try {
            Dialog colorPickerDialog = new Dialog(requireContext());
            colorPickerDialog.setContentView(R.layout.dialog_color_picker);

            // Make the dialog background transparent
            if (colorPickerDialog.getWindow() != null) {
                colorPickerDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                colorPickerDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }

            // Initialize views
            View previewColorView = colorPickerDialog.findViewById(R.id.preview_color_view);
            TextView hexValue = colorPickerDialog.findViewById(R.id.hex_value);
            TextView percentValue = colorPickerDialog.findViewById(R.id.percent_value);

            SeekBar seekBarRed = colorPickerDialog.findViewById(R.id.seekbar_red);
            SeekBar seekBarGreen = colorPickerDialog.findViewById(R.id.seekbar_green);
            SeekBar seekBarBlue = colorPickerDialog.findViewById(R.id.seekbar_blue);

            TextView redValue = colorPickerDialog.findViewById(R.id.red_value);
            TextView greenValue = colorPickerDialog.findViewById(R.id.green_value);
            TextView blueValue = colorPickerDialog.findViewById(R.id.blue_value);

            Button btnCancel = colorPickerDialog.findViewById(R.id.btn_cancel);
            Button btnSelect = colorPickerDialog.findViewById(R.id.btn_select_color);

            // Check if all views are found
            if (previewColorView == null || hexValue == null || percentValue == null ||
                    seekBarRed == null || seekBarGreen == null || seekBarBlue == null ||
                    redValue == null || greenValue == null || blueValue == null ||
                    btnCancel == null || btnSelect == null) {
                Toast.makeText(requireContext(), "Error loading color picker", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get current color components
            int red = Color.red(selectedColorInt);
            int green = Color.green(selectedColorInt);
            int blue = Color.blue(selectedColorInt);

            // Set initial values
            seekBarRed.setProgress(red);
            seekBarGreen.setProgress(green);
            seekBarBlue.setProgress(blue);

            redValue.setText(String.valueOf(red));
            greenValue.setText(String.valueOf(green));
            blueValue.setText(String.valueOf(blue));

            updateColorPreview(previewColorView, hexValue, percentValue, red, green, blue);

            // SeekBar listeners
            SeekBar.OnSeekBarChangeListener seekBarListener = new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int r = seekBarRed.getProgress();
                    int g = seekBarGreen.getProgress();
                    int b = seekBarBlue.getProgress();

                    redValue.setText(String.valueOf(r));
                    greenValue.setText(String.valueOf(g));
                    blueValue.setText(String.valueOf(b));

                    updateColorPreview(previewColorView, hexValue, percentValue, r, g, b);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            };

            seekBarRed.setOnSeekBarChangeListener(seekBarListener);
            seekBarGreen.setOnSeekBarChangeListener(seekBarListener);
            seekBarBlue.setOnSeekBarChangeListener(seekBarListener);

            btnCancel.setOnClickListener(v -> colorPickerDialog.dismiss());

            btnSelect.setOnClickListener(v -> {
                int r = seekBarRed.getProgress();
                int g = seekBarGreen.getProgress();
                int b = seekBarBlue.getProgress();

                selectedColorInt = Color.rgb(r, g, b);

                // Update the custom color chip to show the selected color
                ImageView customColorChip = getView().findViewById(R.id.color_custom);
                if (customColorChip != null) {
                    // Set the background tint to show the selected color
                    customColorChip.setBackgroundTintList(android.content.res.ColorStateList.valueOf(selectedColorInt));

                    // Update selection visuals
                    if (selectedColorView != null) {
                        selectedColorView.setBackgroundResource(R.drawable.bg_color_circle);
                        selectedColorView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    }

                    customColorChip.setBackgroundResource(R.drawable.bg_color_circle);
                    customColorChip.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100).start();
                    selectedColorView = customColorChip;
                }

                colorPickerDialog.dismiss();
                Log.d("CreateJarDialog", "Custom color selected: " + selectedColorInt);
            });

            colorPickerDialog.show();

        } catch (Exception e) {
            Log.e("CreateJarDialog", "Error opening color picker: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error opening color picker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateColorPreview(View previewView, TextView hexValue, TextView percentValue, int red, int green, int blue) {
        int color = Color.rgb(red, green, blue);

        // Update preview color
        if (previewView.getBackground() instanceof GradientDrawable) {
            GradientDrawable drawable = (GradientDrawable) previewView.getBackground();
            drawable.setColor(color);
        } else {
            previewView.setBackgroundColor(color);
        }

        // Update hex value
        String hex = String.format("#%02X%02X%02X", red, green, blue);
        hexValue.setText(hex);

        // Calculate brightness percentage
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        int brightnessPercent = Math.round(hsv[2] * 100);
        percentValue.setText(brightnessPercent + "%");
    }

    private void setupDeadlinePicker() {
        deadlineInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar calendar = Calendar.getInstance();
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                        (view, year1, month1, dayOfMonth) -> {
                            calendar.set(year1, month1, dayOfMonth);

                            // Format for display (what user sees)
                            SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                            selectedDeadlineDisplay = displayFormat.format(calendar.getTime());

                            // Format for storage (what goes to Firestore)
                            SimpleDateFormat storageFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            selectedDeadlineStorage = storageFormat.format(calendar.getTime());

                            // Show the display format to user
                            deadlineInput.setText(selectedDeadlineDisplay);

                            Log.d("CreateJarDialog", "Deadline selected - Display: " + selectedDeadlineDisplay +
                                    ", Storage: " + selectedDeadlineStorage);
                        }, year, month, day);
                datePickerDialog.show();
            }
        });
    }

    private void setupAmountFormatting() {
        targetAmountInput.addTextChangedListener(new TextWatcher() {
            private String current = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(current)) {
                    targetAmountInput.removeTextChangedListener(this);

                    // Allow numbers and dot
                    String cleanString = s.toString().replaceAll("[^\\d.]", "");

                    // Prevent multiple dots
                    if (cleanString.chars().filter(ch -> ch == '.').count() > 1) {
                        cleanString = cleanString.substring(0, cleanString.length() - 1);
                    }

                    if (!cleanString.isEmpty() && !cleanString.equals(".")) {
                        try {
                            // Get currency symbol safely
                            String symbol = getCurrencySymbolSafely();

                            if (cleanString.contains(".")) {
                                current = symbol + cleanString;
                            } else {
                                double parsed = Double.parseDouble(cleanString);
                                current = symbol + String.format(Locale.US, "%,.0f", parsed);
                            }
                            targetAmountInput.setText(current);
                            targetAmountInput.setSelection(current.length());
                        } catch (Exception e) {
                            Log.e("CreateJarDialog", "Error formatting amount: " + e.getMessage());
                            // Fallback to simple formatting
                            current = "₱" + cleanString;
                            targetAmountInput.setText(current);
                            targetAmountInput.setSelection(current.length());
                        }
                    } else {
                        current = "";
                        targetAmountInput.setText("");
                    }
                    targetAmountInput.addTextChangedListener(this);
                }
            }
        });
    }

    private String getCurrencySymbolSafely() {
        try {
            if (getContext() != null) {
                String currencyName = SettingHelper.getCurrency(getContext());
                return SettingHelper.getCurrencySymbol(currencyName);
            }
        } catch (Exception e) {
            Log.e("CreateJarDialog", "Error getting currency symbol: " + e.getMessage());
        }
        return "₱"; // Default fallback
    }

    private void setupCreateButton(View view) {
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String jarName = jarNameInput.getText().toString().trim();
                String targetAmount = targetAmountInput.getText().toString().trim();

                if (jarName.isEmpty()) {
                    jarNameInput.setError(getString(R.string.jar_name_required));
                    return;
                }

                String cleanAmount = targetAmount.replaceAll("[^\\d.]", "");
                if (cleanAmount.isEmpty() || cleanAmount.equals("0") || cleanAmount.equals(".")) {
                    targetAmountInput.setError(getString(R.string.error_invalid_amount));
                    return;
                }

                int selectedChipId = jarTypeGroup.getCheckedChipId();
                String jarType = "Flexible";
                if (selectedChipId == R.id.mandatory_chip) {
                    jarType = "Mandatory";
                } else if (selectedChipId == R.id.flexible_chip) {
                    jarType = "Flexible";
                } else if (selectedChipId == R.id.shared_chip) {
                    jarType = "Shared";
                }

                Bundle result = new Bundle();
                result.putString(KEY_JAR_NAME, jarName);
                result.putString(KEY_TARGET_AMOUNT, cleanAmount);
                result.putString(KEY_JAR_TYPE, jarType);
                result.putString(KEY_SELECTED_ICON, selectedIconName);
                result.putInt(KEY_SELECTED_COLOR, selectedColorInt);

                // Use the storage format date (yyyy-MM-dd) for Firestore
                if (selectedDeadlineStorage != null && !selectedDeadlineStorage.isEmpty()) {
                    result.putString(KEY_DEADLINE, selectedDeadlineStorage);
                    Log.d("CreateJarDialog", "Sending deadline to fragment: " + selectedDeadlineStorage);
                }

                // Add logging to debug
                Log.d("CreateJarDialog", "Sending icon name: " + selectedIconName);

                getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
                dismiss();
            }
        });
    }

    private void setupCloseButton(View view) {
        ImageView closeButton = view.findViewById(R.id.close_button);
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> dismiss());
        }
    }
}