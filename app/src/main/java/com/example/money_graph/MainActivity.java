package com.example.money_graph;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private Uri selectedFileUri;
    private final ActivityResultLauncher<Intent> csvFilePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        selectedFileUri = data.getData(); // ← Сохраняем URI
                        startFileProcessing(); // ← Запускаем обработку в фоне
                    }
                }
            }
    );

    // Объявляем элементы интерфейса, чтобы использовать их в разных методах
    private LinearLayout all_text;
    private TextView resultTextView;
    private TextView resultTextView_left;
    private TextView resultTextView_right;
    private Button importButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Связываем с макетом

        // 1. Находим элементы интерфейса
        importButton = findViewById(R.id.import_button);
        resultTextView = findViewById(R.id.result_text_view);
        resultTextView_left = findViewById(R.id.textView_left);
        resultTextView_right = findViewById(R.id.textView_right);
        all_text = findViewById(R.id.all_text);

        // 2. Вешаем обработчик на кнопку
        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker();
            }
        });
    }

    // Метод для открытия файлового менеджера
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.setType("*/*");

        String[] mimeTypes = {
                "text/csv",
                "text/comma-separated-values",
                "application/csv",
                "application/vnd.ms-excel" // иногда CSV ассоциируется с Excel
        };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        csvFilePickerLauncher.launch(intent);
    }

    private void startFileProcessing() {
        // Показываем индикатор загрузки
        importButton.setClickable(false);
        importButton.setAlpha(0);

        // Запускаем в отдельном потоке
        new Thread(() -> {
            try {
                // Даем время анимации загрузки запуститься
                Thread.sleep(100);

                // Обрабатываем файл
                processSelectedFile();

            } catch (Exception e) {
                runOnUiThread(() -> {
                    resultTextView.setText("Ошибка: " + e.getMessage());
                });
            }
        }).start();
    }

    private void processSelectedFile() {
        try {
            runOnUiThread(() -> resultTextView.setText("Открытие файла..."));

            InputStream inputStream = getContentResolver().openInputStream(selectedFileUri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            boolean isFirstLine = true;
            List<Simple_transaction> all_Transaction = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("d.M.yyyy", Locale.getDefault());
            sdf.setLenient(true);
            int lineCount = 0;

            runOnUiThread(() -> resultTextView.setText("Чтение CSV..."));

            // Чтение файла
            while ((line = reader.readLine()) != null) {
                lineCount++;
                if (lineCount % 100 == 0) {
                    final int currentCount = lineCount;
                    runOnUiThread(() ->
                            resultTextView.setText("Чтение строк: " + currentCount)
                    );
                }

                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                String cleanLine = line.replace("\"", "");
                String[] columns = cleanLine.split(",");

                if (columns.length >= 6) {
                    try {
                        Date date = sdf.parse(columns[0].trim());
                        String type = columns[1].trim();
                        String bank_account_from = columns[2].trim();
                        String transfer_to = columns[3].trim();
                        double transfer_amount = Double.parseDouble(columns[4].trim());
                        String currency = columns[5].trim();
                        String transfer_note = columns.length > 9 ? columns[9].trim() : "";

                        all_Transaction.add(new Simple_transaction(date, type, bank_account_from,
                                transfer_to, transfer_amount,
                                currency, transfer_note));

                    } catch (Exception e) {
                        Log.w("CSV", "Ошибка в строке " + lineCount + ": " + e.getMessage());
                    }
                }
            }

            reader.close();
            inputStream.close();

            runOnUiThread(() -> resultTextView.setText("Обработка данных..."));

            // →→→ ВСЯ ДАЛЬНЕЙШАЯ ОБРАБОТКА В ФОНЕ ←←←

            // 1. Реверс списка
            Collections.reverse(all_Transaction);

            // 2. Первичная обработка
            processAllDataAfterLoading(all_Transaction);

            // 3. Создание второго списка
            runOnUiThread(() -> resultTextView.setText("Создание баланса..."));
            List<Balance_transaction> balanceChanges = processCalcChangesOfBalance(all_Transaction);

            // 4. Подсчет статистики
            runOnUiThread(() -> resultTextView.setText("Расчет статистики..."));
            int totalCount_first = all_Transaction.size();
            int totalCount_second = balanceChanges.size();
            double totalSum = roundDecimal(calculateTotalSum(balanceChanges));
            double[] list_totalSum = calculateTotalSums(balanceChanges);

            // 5. Подготовка данных для графика
            runOnUiThread(() -> resultTextView.setText("Подготовка графика..."));
            List<Double> values = Arrays.asList(list_totalSum[0], list_totalSum[1], list_totalSum[2],
                    list_totalSum[3], list_totalSum[4]);

            // 6. Обновление UI в главном потоке
            runOnUiThread(() -> {
                // Показываем основной текст
                all_text.setAlpha(1);

                // Обновляем TextViews
                resultTextView.setText("Всего транзакций: " + totalCount_first +
                        "\nТранзакций копилки: " + totalCount_second +
                        "\nИтоговый долг: " + totalSum + "\n");

                resultTextView_left.setText("Недельный:\nДолг:\nПолучка:\nОкругление:\nНеустойка:");
                resultTextView_right.setText(
                        String.format(Locale.US, "%.2f\n%.2f\n%.2f\n%.2f\n%.2f",
                                list_totalSum[0], list_totalSum[1], list_totalSum[2],
                                list_totalSum[3], list_totalSum[4])
                );

                // Обновляем график
                DonutChartView donutChart = findViewById(R.id.donutChart);
                donutChart.setDonutWidth(50f);
                donutChart.setDonutPieSkip(4f);
                donutChart.setData(values);

                // Скрываем загрузку
                showLoading(false);

                Log.d("TOTAL", "Общая сумма всех операций: " + totalSum);
                Log.d("TOTAL", "Недельный: " + list_totalSum[0]);
                Log.d("TOTAL", "Долг: " + list_totalSum[1]);
                Log.d("TOTAL", "Получка: " + list_totalSum[2]);
                Log.d("TOTAL", "Округление: " + list_totalSum[3]);
                Log.d("TOTAL", "Неустойка: " + list_totalSum[4]);
            });

        } catch (Exception e) {
            runOnUiThread(() -> {
                showLoading(false);
                resultTextView.setText("Ошибка: " + e.getMessage());
                Log.e("PROCESS", "Ошибка обработки", e);
            });
        }
    }

    private void showLoading(boolean show) {
        runOnUiThread(() -> {
            if (show) {
                all_text.setAlpha(0);
                importButton.setAlpha(0);
            } else {
                all_text.setAlpha(1);
            }
        });
    }

    private void readCsvFile(Uri fileUri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(fileUri);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String line;
        boolean isFirstLine = true;
        List<Simple_transaction> all_Transaction = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("d.M.yyyy", Locale.getDefault());
        sdf.setLenient(true);

        all_text.setAlpha(0);
        importButton.setAlpha(0);

        while ((line = reader.readLine()) != null) {

            Date date = null;
            String type = "";
            String bank_account_from = "";
            String transfer_to = "";
            String currency = "";
            String transfer_note = "";
            double transfer_amount = 0.0;

            if (isFirstLine) {
                isFirstLine = false;
                continue; // Пропускаем строку с заголовками
            }
//            Log.d("LINE_PARCER_BEFORE", line);
            String cleanLine = line.replace("\"", "");
//            Log.d("LINE_PARCER_CLEAN", cleanLine);
            String[] columns = cleanLine.split(",");

//            Log.d("LINE_PARCER_NUM_COLUMNS", "" + columns.length);

            if (columns.length >= 6) {
                String listString = String.join(" - ", columns);
//                Log.d("LINE_PARCER_COLUMNS", listString);

                try {
                    date = sdf.parse(columns[0].trim());
                } catch (ParseException e) {
                    Log.w("CSV", "Ошибка парсинга даты: " + columns[0]);
                }
                type = columns[1].trim();
                bank_account_from = columns[2].trim();
                transfer_to = columns[3].trim();
                transfer_amount = Double.parseDouble(columns[4].trim());
                currency = columns[5].trim();

                if (columns.length > 9) {
                    transfer_note = columns[9].trim();
                } else {
                    transfer_note = ""; // или какое-то значение по умолчанию
//                    Log.d("LINE_PARCER_WARN", "Нет колонки 9 в строке, используем пустую строку");
                }

//                Log.d("LINE_PARCER", date + " " + type + " " + bank_account_from + " " + transfer_to + " " + transfer_amount + " " + currency + " " + transfer_note);
                all_Transaction.add(new Simple_transaction(date, type, bank_account_from, transfer_to, transfer_amount, currency, transfer_note));
            }
        }

        reader.close();
        inputStream.close();

        Collections.reverse(all_Transaction);

        processAllDataAfterLoading(all_Transaction);
        List<Balance_transaction> balanceChanges = processCalcChangesOfBalance(all_Transaction);

        all_text.setAlpha(1);

        int totalCount_first = all_Transaction.size();
        int totalCount_second = balanceChanges.size();

        double totalSum = roundDecimal(calculateTotalSum(balanceChanges));
        double [] list_totalSum = new double[7];
        list_totalSum = calculateTotalSums(balanceChanges);

        Log.d("TOTAL", "Общая сумма всех операций: " + totalSum);
        resultTextView.setText("Всего транзакций: " + totalCount_first +
                "\nТранзакций копилки: " + totalCount_second +
                "\nИтоговый долг: " + totalSum + "\n");

        resultTextView_left.setText("Недельный:\nДолг:\nПолучка:\nОкругление:\nНеустойка:");
        resultTextView_right.setText(list_totalSum[0]+"\n"+list_totalSum[1]+"\n"+list_totalSum[2]+"\n"+list_totalSum[3]+"\n"+list_totalSum[4]);

        Log.d("TOTAL", "Недельный: " + list_totalSum[0]);
        Log.d("TOTAL", "Долг: " + list_totalSum[1]);
        Log.d("TOTAL", "Получка: " + list_totalSum[2]);
        Log.d("TOTAL", "Округление: " + list_totalSum[3]);
        Log.d("TOTAL", "Неустойка: " + list_totalSum[4]);

        DonutChartView donutChart = findViewById(R.id.donutChart);

        donutChart.setDonutWidth(30f);     // Толщина кольца
        List<Double> values = Arrays.asList(list_totalSum[0], list_totalSum[1], list_totalSum[2], list_totalSum[3], list_totalSum[4]);
        donutChart.setData(values);

    }

    private void processAllDataAfterLoading(List<Simple_transaction> all_Transaction) throws IOException {
        Set<String> uniqueBanks_from = new HashSet<>();
        for (Simple_transaction transaction : all_Transaction) {
            uniqueBanks_from.add(transaction.getBank_account_from());
        }
        List<String> uniqueBankFromList = new ArrayList<>(uniqueBanks_from);
        for (String bank : uniqueBankFromList) {
//            Log.d("UNIQUE_BANKS_FROM", "Банк: " + bank);
        }

        Set<String> uniqueBanks_to = new HashSet<>();
        for (Simple_transaction transaction : all_Transaction) {
            uniqueBanks_to.add(transaction.getBank_account_from());
        }
    }

    private List<Balance_transaction> processCalcChangesOfBalance(List<Simple_transaction> allTransactions) {
        List<Balance_transaction> summaryList = new ArrayList<>();

        int[] count_of_operation = new int[7];

        SimpleDateFormat sdf = new SimpleDateFormat("d.M.yyyy", Locale.getDefault());
        Date currentMonday;
        try {
            currentMonday = sdf.parse("1.6.2020");
        } catch (ParseException e) {
            Log.e("DATE", "Ошибка созния нулевого понедельника");
            currentMonday = new Date();
        }

        for (int i = 0; i < allTransactions.size(); i++) {
            Simple_transaction transaction = allTransactions.get(i);
            Date transactionDate = transaction.getDate();

            while (isDateAfterNextMonday(transactionDate, currentMonday)) {
                Balance_transaction calculated = new Balance_transaction(
                        "Недельный приход",
                        currentMonday,
                        -1,
                        250,
                        "RUB"
                );
                summaryList.add(calculated);
                count_of_operation[0]++;

                currentMonday = addDays(currentMonday, 7);
            }

//            Log.d("LINE_PARCER_IN_CALC",
//                    transaction.getDate() + " " +
//                        transaction.getType() + " " +
//                        transaction.getBank_account_from() + " " +
//                        transaction.getTransfer_to() + " " +
//                        transaction.getTransfer_amount() + " " +
//                        transaction.getCurrency() + " " +
//                        transaction.getTransfer_note());

            if ("Накопления".equals(transaction.getBank_account_from())) {
                Balance_transaction calculated = new Balance_transaction(
                        "Долг",
                        transaction.getDate(),
                        i,
                        roundDecimal(transaction.getTransfer_amount()),
                        transaction.getCurrency()
                );
                summaryList.add(calculated);
                count_of_operation[1]++;
            }
            if ("Доход".equals(transaction.getType())) {
                Balance_transaction calculated = new Balance_transaction(
                        "Получка",
                        transaction.getDate(),
                        i,
                        roundDecimal(transaction.getTransfer_amount() / 10),
                        transaction.getCurrency()
                );
                summaryList.add(calculated);
                count_of_operation[2]++;
            }
            if ("Расход".equals(transaction.getType()) && !"Неустойка".equals(transaction.getTransfer_to())) {
                Balance_transaction calculated = new Balance_transaction(
                        "Округление",
                        transaction.getDate(),
                        i,
                        roundDecimal(calcRoundedAmount(transaction.getTransfer_amount())),
                        transaction.getCurrency()
                );
                summaryList.add(calculated);
                count_of_operation[3]++;
            }
            if ("Неустойка".equals(transaction.getTransfer_to())) {
                Balance_transaction calculated = new Balance_transaction(
                        "Неустойка",
                        transaction.getDate(),
                        i,
                        roundDecimal(transaction.getTransfer_amount()),
                        transaction.getCurrency()
                );
                summaryList.add(calculated);
                count_of_operation[4]++;
            }
            if ("Расход".equals(transaction.getType()) && "ZZZ".equals(transaction.getTransfer_note())) {
                Balance_transaction calculated = new Balance_transaction(
                        "Трата",
                        transaction.getDate(),
                        i,
                        roundDecimal(transaction.getTransfer_amount() * (-0.5)),
                        transaction.getCurrency()
                );
                summaryList.add(calculated);
                count_of_operation[5]++;
            }
            if ("Перевод".equals(transaction.getType()) && "Накопления".equals(transaction.getTransfer_to())) {
                Balance_transaction calculated = new Balance_transaction(
                        "Возмещение",
                        transaction.getDate(),
                        i,
                        roundDecimal(transaction.getTransfer_amount() * (-1)),
                        transaction.getCurrency()
                );
                summaryList.add(calculated);
                count_of_operation[6]++;
            }
        }

        Log.d("SUMMARY", "Всего записей в summary: " + summaryList.size());
        for (int k = 0; k < 7; k++) {
            Log.d("SUMMARY", "" + count_of_operation[k]);
        }
        return summaryList;
    }

    private boolean isDateAfterNextMonday(Date date, Date currentMonday) {
        Date nextMonday = addDays(currentMonday, 7);
        return date.after(nextMonday);
    }

    // Добавляет дни к дате
    private Date addDays(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_YEAR, days);
        return calendar.getTime();
    }

    private double calcRoundedAmount(double amount) {
        if (amount <= 49.99) {
            return (roundUpToMultiple(amount, 10) - amount);
        } else if (amount <= 299.99) {
            return (roundUpToMultiple(amount, 50) - amount);
        } else if (amount <= 1999.99) {
            return (roundUpToMultiple(amount, 100) - amount);
        } else if (amount <= 9999.99) {
            return (roundUpToMultiple(amount, 500) - amount);
        } else {
            return (roundUpToMultiple(amount, 1000) - amount);
        }
    }

    private double roundUpToMultiple(double value, int multiple) {
        return Math.ceil(value / multiple) * multiple;
    }

    private double calculateTotalSum(List<Balance_transaction> summaryList) {
        double total = 0;

        for (Balance_transaction transaction : summaryList) {
            total += transaction.getAmount();
        }

        return total;
    }

    private double [] calculateTotalSums(List<Balance_transaction> summaryList) {
        double[] total = new double[7];

        for (Balance_transaction transaction : summaryList) {
            switch (transaction.getType()) {
                case "Недельный приход":
                    total[0] += transaction.getAmount();
                    break;
                case "Долг":
                    total[1] += transaction.getAmount();
                    break;
                case "Получка":
                    total[2] += transaction.getAmount();
                    break;
                case "Округление":
                    total[3] += transaction.getAmount();
                    break;
                case "Неустойка":
                    total[4] += transaction.getAmount();
                    break;
                case "Трата":
                    total[5] += transaction.getAmount();
                    break;
                case "Возмещение":
                    total[6] += transaction.getAmount();
                    break;
            }
        }

        for (int i = 0; i < 7; i++) {
            total[i] = roundDecimal(total[i]);
        }

        return total;
    }

    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        return sdf.format(date);
    }

    private double roundDecimal(double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}

