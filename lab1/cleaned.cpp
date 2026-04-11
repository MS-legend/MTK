#include <iostream>
#include <string>
// Однострочный комментарий: глобальные переменные
int globalVar = 100;
/* Многострочный комментарий:
Объявление функции сложения */
int add(int x, int y) {
return x + y;
}
// Функция для проверки четности
bool isEven(int num) {
return (num % 2 == 0);
}
int main() {
// Объявление переменных
int a = 10;
int b = 20;
int result;
// Присваивание
result = a + b;
// Арифметические выражения
int product = a * b;
int diff = b - a;
// Логические выражения
bool condition1 = (a > 5) && (b < 30);
bool condition2 = (a == 10) || (b == 25);
// Условный оператор if-else
if (condition1) {
std::cout << "Условие 1 истинно" << std::endl;
} else {
std::cout << "Условие 1 ложно" << std::endl;
}
// Цикл for
for (int i = 0; i < 5; i++) {
std::cout << "i = " << i << std::endl;
}
// Цикл while
int counter = 0;
while (counter < 3) {
std::cout << "counter = " << counter << std::endl;
counter++;
}
// Объявление и вызов функции
int sum = add(a, b);
std::cout << "Сумма: " << sum << std::endl;
// Еще один вызов
if (isEven(sum)) {
std::cout << "Сумма четная" << std::endl;
}
return 0;
}
