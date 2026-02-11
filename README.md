# PR1
Codi base de la PR1 de l'assignatura de Software Distribuït de la UB

## Enunciat de pràctiques

Repositori Github a l'[Enunciat de pràctiques](https://github.com/SoftwareDistribuitUB-2026/Enunciat-PR1)

## Com està estructurat el codi
El codi està estructurat en tres parts:
- **client**: Conté el codi del client que s'encarrega de fer les peticions al servidor.
- **server**: Conté el codi del servidor que s'encarrega de rebre les peticions del client i respondre-les.
- **comUtils**: Conté el codi comú que comparteixen el client i el servidor.


## Com compilar, encapsular i executar el codi
Per executar el codi cal tenir instal·lat el JDK de Java. Un cop instal·lat, es pot executar el codi de la següent manera:

### Servidor
```bash 
mvn clean package
java -jar target/Server-1.0-SNAPSHOT-jar-with-dependencies.jar -p 8080
```

### Client

```bash 
mvn clean package
java -jar target/Client-1.0-SNAPSHOT-jar-with-dependencies.jar -h localhost -p 8080
```

