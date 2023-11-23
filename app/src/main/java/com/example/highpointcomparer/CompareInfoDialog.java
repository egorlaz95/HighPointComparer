package com.example.highpointcomparer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class CompareInfoDialog extends BottomSheetDialogFragment {
    private double elevationDifference;
    public String[] compObject = {"Шесть бутылок вина", "Статуя Давида", "Пирамида Луи XIV во дворце Версаль",
            "Хрущёвка","Сфинкс в Гизе", "Луксорский обелиск", "Водопад Перун", "Статуя Христа-Искупителя",
            "Водопад Пульхапанзак", "Статуя Свободы",
    "Монумент африканского возрождения", "Башня Сююмбике", "Золотая обитель Будды",
    "Монумент ракета-носитель «Союз» в Самаре", "Кутб-Минар",
    "Гора Паасо", "Алексеевская сопка", "Статуя Родина-мать зовёт!",
    "Замок Нойшванштайн", "Байтерек", "Большая арка Дефанс", "Лондонский глаз",
    "Дубайская рамка", "Spinnaker Tower", "Ростовская телебашня", "Башня Монпарнас",
    "Шан-Кая", "Дубайский глаз", "Гора Намсан", "Американ-Интернешнл-билдинг",
    "Эйфелева Башня", "Ташкентская телебашня",
    "Эмпайр Стейт Билдинг", "Мост Пули", "CTF Finance Centre",
    "Миттелаллалин", "Шанхайская башня", "Гора Крузенштерна",
    "Лахта-Центр", "Парк на горе Мтацимнда", "Бурдж Халифа", "Гора Маунт",
    "Гора Кагаташ", "Радиотелескоп в Зеленчуке", "Планирующийся небоскреб Azerbaijan Tower",
            "Гора Ай-Петри", "Пятисотэтажный дом", "Гора Хуэйлун Тяньцзе",
            "эколагерь Нахазо", "Красная Поляна", "Самый высокий отель в мире",
            "Горный хребет Домбай", "Эльбрус", "Гора Цаст", "Альпы",
            "Гора Виникунка", "Вершина Пик Орисаба", "Вершина Мак-Кинли",
            "Вершина Ама-Даблам", "Пик Ленина", "Горный массив Канченджанга", "Эверест"
    };

    public CompareInfoDialog(double elevationDifference) {
        this.elevationDifference = elevationDifference;
    }

    public String landmark(){
        String landmarkWeNeed = null;
        int elevation = (int) elevationDifference;
        int[] elevationRanges = {0, 3, 6, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90,
        95, 100, 120, 140, 160, 180, 200, 250, 300, 350, 400, 450, 500, 550, 600, 650, 700, 750, 800, 850,
        900, 950, 1000, 1200, 1400, 1600, 1800, 2000, 2500, 3000, 3500, 4000, 4500, 5000, 5500, 6000,
        6500, 7000, 7500, 8000, 8848};
        int landmarkIndex = -1;
        for (int i = 0; i < elevationRanges.length; i++) {
            if (elevation > elevationRanges[i] && elevation <= elevationRanges[i + 1]) {
                landmarkIndex = i;
                break;
            }
        }
        if (landmarkIndex != -1) {
            landmarkWeNeed = compObject[landmarkIndex];
        }
        return landmarkWeNeed;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.compare_info, container, false);
        TextView textView = view.findViewById(R.id.textViewHeightDifference);
        textView.setText("Разница в высоте: " + elevationDifference + " м" + "\n" +
                "Эта высота приблизительно равна высоте достопримечательности: " + landmark());
        return view;
    }
}
