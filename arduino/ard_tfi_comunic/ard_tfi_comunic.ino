// Firmware VLC: transmite con el LED, recibe con la LDR y devuelve
// por USB los bytes recibidos. NO imprimir texto por Serial: la
// aplicación Java interpreta todo lo que llega como trama.

const int LED = 8;               // LED emisor
const int LDR = A0;              // sensor receptor
const unsigned int BIT_MS = 100; // tiempo de bit (100 a 150 ms)

int umbral = 500;
bool luzEsAlto = true;           // true si la lectura sube cuando hay luz

int promedioLDR(int n) {
  long suma = 0;
  for (int i = 0; i < n; i++) {
    suma += analogRead(LDR);
    delay(10);
  }
  return suma / n;
}

void calibrar() {
  digitalWrite(LED, LOW);
  delay(500);                    // la LDR es lenta: dejar que se estabilice
  int apagado = promedioLDR(20);
  digitalWrite(LED, HIGH);
  delay(500);
  int encendido = promedioLDR(20);
  digitalWrite(LED, LOW);

  luzEsAlto = (encendido > apagado);
  umbral = (apagado + encendido) / 2;

  // Contraste bajo: el LED integrado (pin 13) titila 5 veces
  if (abs(encendido - apagado) < 50) {
    for (int i = 0; i < 5; i++) {
      digitalWrite(LED_BUILTIN, HIGH); delay(150);
      digitalWrite(LED_BUILTIN, LOW);  delay(150);
    }
  }
}

bool leerBit() {
  int v = analogRead(LDR);
  return luzEsAlto ? (v > umbral) : (v < umbral);
}

void enviarYRecibir(byte b) {
  byte rx = 0;

  // Bit de INICIO (LED encendido)
  digitalWrite(LED, HIGH);
  delay(BIT_MS / 2);
  leerBit();                     // muestra del bit de inicio (no se usa)
  delay(BIT_MS - BIT_MS / 2);

  // 8 bits de DATOS, LSB primero
  for (int i = 0; i < 8; i++) {
    digitalWrite(LED, (b >> i) & 1);
    delay(BIT_MS / 2);           // muestreo centrado en el bit
    if (leerBit()) rx |= (1 << i);
    delay(BIT_MS - BIT_MS / 2);
  }

  // Bit de PARADA (LED apagado)
  digitalWrite(LED, LOW);
  delay(BIT_MS);

  Serial.write(rx);              // devuelve el byte recibido por la luz
}

void setup() {
  pinMode(LED, OUTPUT);
  pinMode(LED_BUILTIN, OUTPUT);
  Serial.begin(9600);
  calibrar();
}

void loop() {
  if (Serial.available() > 0) {
    enviarYRecibir(Serial.read());
  }
}