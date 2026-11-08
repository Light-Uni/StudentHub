package vn.edu.studentmanager.utils;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import java.time.LocalDate;
import java.util.*;

public class FormFields {
    private final Context context;
    private final LinearLayout container;
    private final Map<String, EditText> inputs = new LinkedHashMap<>();
    private final Map<String, String[]> codes = new HashMap<>(), labels = new HashMap<>();
    public FormFields(Context context, LinearLayout container) { this.context = context; this.container = container; }
    public EditText text(String key, String label, String value, int type) {
        EditText input = Ui.field(context, container, label, value, type); input.setId(View.generateViewId()); inputs.put(key, input); return input;
    }
    public void date(String key, String label, String value) {
        EditText input = text(key, label, value, InputType.TYPE_CLASS_DATETIME);
        input.setFocusable(false); input.setOnClickListener(view -> {
            LocalDate date; try { date = LocalDate.parse(Ui.value(input)); } catch (Exception e) { date = LocalDate.now(); }
            DatePickerDialog dialog = new DatePickerDialog(context, (picker, y, m, d) -> input.setText(LocalDate.of(y, m + 1, d).toString()), date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());
            dialog.setButton(android.content.DialogInterface.BUTTON_NEUTRAL, "Xóa ngày", (d, w) -> input.setText("")); dialog.show();
        });
    }
    public void select(String key, String label, String value, String[] values, String[] display) {
        TextInputLayout box = new TextInputLayout(context, null, com.google.android.material.R.attr.textInputStyle);
        box.setHint(label); box.setEndIconMode(TextInputLayout.END_ICON_DROPDOWN_MENU);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = Ui.dp(context, 16); box.setLayoutParams(lp);
        MaterialAutoCompleteTextView input = new MaterialAutoCompleteTextView(context); input.setInputType(InputType.TYPE_NULL);
        input.setMinHeight(Ui.dp(context, 56)); input.setSimpleItems(display);
        int index = Arrays.asList(values).indexOf(value); input.setText(display[Math.max(0, index)], false);
        box.addView(input); container.addView(box); inputs.put(key, input); codes.put(key, values); labels.put(key, display);
    }
    public Map<String, Object> values() {
        Map<String, Object> result = new LinkedHashMap<>();
        inputs.forEach((key, input) -> {
            String value = Ui.value(input);
            if (codes.containsKey(key)) { int index = Arrays.asList(labels.get(key)).indexOf(value); value = index < 0 ? "" : codes.get(key)[index]; }
            result.put(key, value);
        }); return result;
    }
    public void save(Bundle bundle) { inputs.forEach((key, input) -> bundle.putString("field_" + key, input.getText().toString())); }
    public void restore(Bundle bundle) {
        if (bundle == null) return;
        inputs.forEach((key, input) -> { if (bundle.containsKey("field_" + key)) {
            if (input instanceof AutoCompleteTextView auto) auto.setText(bundle.getString("field_" + key), false);
            else input.setText(bundle.getString("field_" + key));
        }});
    }
    public void enabled(boolean enabled) { inputs.values().forEach(input -> input.setEnabled(enabled)); }
}
