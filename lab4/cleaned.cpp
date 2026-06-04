#include "iostream"
#include "string"

int globalVar = 100;

int add(int x, int y) {
return x + y;
}

bool isEven(int num) {
return (num % 2 == 0);
}

int main() {
int a = 10;
int b = 20;
int result;

result = a + b;

int product = a * b;
int diff = b - a;

bool condition1 = (a > 5) && (b < 30);
bool condition2 = (a == 10) || (b == 25);

if (condition1) {
std::cout << "Условие 1 истинно" << std::endl;
} else {
std::cout << "Условие 1 ложно" << std::endl;
}

for (int i = 0; i < 5; i++) {
std::cout << "i = " << i << std::endl;
}

int counter = 0;
while (counter < 3) {
std::cout << "counter = " << counter << std::endl;
counter++;
}

int sum = add(a, b);
std::cout << "Сумма: " << sum << std::endl;

if (isEven(sum)) {
std::cout << "Сумма четная" << std::endl;
}

return 0;
}