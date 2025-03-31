package ru.nsu.palkin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class C3Linearization {
    /**
     * Вычисляет порядок обхода классов (C3‑линеаризацию) для переданного класса.
     * При этом порядок базовых классов берётся из аннотации @MultiInheritance.
     *
     * @param cls Класс, для которого вычисляем линейаризацию
     * @return Список классов в порядке обхода
     */
    public static List<Class<?>> c3Linearization(Class<?> cls) {
        // Получаем список непосредственных родителей из аннотации (если есть)
        List<Class<?>> bases = getParentsFromAnnotation(cls);

        List<List<Class<?>>> linearizations = new ArrayList<>();
        // Рекурсивно вычисляем линейаризацию для каждого родителя
        for (Class<?> parent : bases) {
            linearizations.add(c3Linearization(parent));
        }
        // Линейаризация для текущего класса: добавляем его в начало списка
        List<Class<?>> selfList = new ArrayList<>();
        selfList.add(cls);
        linearizations.add(0, selfList);

        return merge(linearizations);
    }

    /**
     * Объединяет списки линейаризаций по алгоритму C3.
     */
    private static List<Class<?>> merge(List<List<Class<?>>> seqs) {
        List<Class<?>> result = new ArrayList<>();
        while (true) {
            // Удаляем пустые списки
            seqs.removeIf(List::isEmpty);
            if (seqs.isEmpty()) {
                return result;
            }
            Class<?> candidate = null;
            for (List<Class<?>> seq : seqs) {
                Class<?> head = seq.get(0);
                if (isGoodHead(head, seqs)) {
                    candidate = head;
                    break;
                }
            }
            if (candidate == null) {
                throw new RuntimeException("Невозможно создать консистентный порядок (C3 линейаризация).");
            }
            result.add(candidate);
            // Удаляем найденного кандидата из начала всех последовательностей
            for (List<Class<?>> seq : seqs) {
                if (!seq.isEmpty() && seq.get(0).equals(candidate)) {
                    seq.remove(0);
                }
            }
        }
    }

    /**
     * Проверяет, что кандидат не встречается во всех последовательностях, кроме первого элемента.
     */
    private static boolean isGoodHead(Class<?> candidate, List<List<Class<?>>> seqs) {
        for (List<Class<?>> seq : seqs) {
            if (seq.indexOf(candidate) > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Извлекает родителей для класса через аннотацию @MultiInheritance.
     * Если аннотация отсутствует или список пустой, возвращает пустой список.
     */
    private static List<Class<?>> getParentsFromAnnotation(Class<?> cls) {
        MultiInheritance mi = cls.getAnnotation(MultiInheritance.class);
        if (mi != null) {
            return new ArrayList<>(Arrays.asList(mi.value()));
        }
        return new ArrayList<>();
    }

}
