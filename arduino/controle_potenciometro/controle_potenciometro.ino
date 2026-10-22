const int PINO_SENSOR = A0;
const int LEITURAS_POR_MEDIA = 4;
const int INTERVALO_MS = 20;

void setup() {
  Serial.begin(9600);
}

void loop() {
  long soma = 0;
  for (int i = 0; i < LEITURAS_POR_MEDIA; i++) {
    soma += analogRead(PINO_SENSOR);
  }
  int valor = soma / LEITURAS_POR_MEDIA;

  Serial.println(valor);
  delay(INTERVALO_MS);
}
