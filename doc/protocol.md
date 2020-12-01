# Opzetten Verbinding
Bij het verbinden met de server op poort 1337 krijg je een `INFO Welcome` bericht terug.

Na verbinden moet je inloggen.

Anders krijg je de error:
```shell script
s: 403 Please login first
```

c = Client
s = Server
e = Everyone
o = Everyone but you (Others)

Status codes:
2xx = Positive
4xx = Error

# Login
Om in te loggen moet je een `CONN [username]` bericht sturen.

### Happy Flow

```shell script
c: CONN Quentin
s: 200 Logged in as Quentin
o: 204 Quentin joined the server
```

### Sad Flow
```shell script
c: CONN [name that is already in use]
s: 401 User with username 'Quentin' is already logged in
```

```shell script
c: CONN [name that does not match requirements]
s: 402 Name should be between 3 and 14 characters and should match [a-zA-Z_0-9]
```

# Bericht Versturen
Als je een bericht naar iedereen op de server wilt versturen dan gebruik je `BCST`.

### Happy Flow

```shell script

```

### Sad Flow
```shell script
c: BCST
s:
```

