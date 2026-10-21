package iq.haji.alhagebb;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private Repo repo;
    private long selectedCategory = -1;
    private List<Models.Category> categories = new ArrayList<>();
    private List<Models.Driver> drivers = new ArrayList<>();
    private final LinkedHashMap<Long, Models.CartLine> cart = new LinkedHashMap<>();
    private String token = UUID.randomUUID().toString();
    private boolean saving = false;

    private LinearLayout categoryColumn;
    private GridLayout dishGrid;
    private TextView dishEmpty;
    private LinearLayout cartList;
    private TextView cartEmpty;
    private EditText phoneField, addressField, discountField, deliveryField;
    private Spinner driverSpinner;
    private TextView subtotalView, totalView;
    private Button saveButton;
    private int gridCols = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        repo = Repo.get(this);
        buildUi();
    }

    @Override
    protected void on
