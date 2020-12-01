# Opzetten Verbinding
Bij het verbinden met de server op poort 1337 krijg je een `INFO Welcome` bericht terug.

Na verbinden moet je inloggen.

Anders krijg je de error:
```shell script
s: 403 Please login first
```

c = Client
s = Server
e = Server to everyone
o = Server to everyone but you (Others)
r = Server to everyone in the room

Status codes:
2xx = Positive
4xx = Error

# Login
Om in te loggen moet je een `CONN [username]` bericht sturen.

### Happy Flow
Ingelogd als Quentin; wordt in de andere flows gebruikt als de ingelogde user.
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
c: BCST Hello World
e: BCST Quentin Hello World
```

### Sad Flow
Geen bericht
```shell script
c: BCST
s: 409 Please give a message to send
```

# Make Room
Maak een room met het `MAKE` commando.

### Happy Flow
```shell script
c: MAKE my_first_room
e: 203 my_first_room
```

### Sad Flow
```shell script
c: MAKE [name that does not match requirements or no name]
s: 402 Room name should be between 3 and 14 characters and should match [a-zA-Z_0-9]
```

# List Rooms
### Happy Flow
De room namen zijn gescheiden door punt-comma's,
daarom mag je geen room aanmaken met een punt-comma in de naam.

```shell script
c: ROOMS [should be empty, but doesnt matter]
s: ROOMS owo;swag;shrek;my_first_room
```
Wanneer er geen rooms zijn word er niets terug gestuurd:
```shell script
c: ROOMS
s: ROOMS
```
### Sad Flows
There's nothing sad about rooms

# Join Room
Als je al in een room zit verlaat je deze automatisch.
Zie [leave room](#leave-room) voor details.

### Happy Flow
```shell script
c: JOIN my_first_room
r: Quentin joined my_first_room
```

### Sad Flow
```shell script
c: JOIN room_bestaat_niet
s: 400 Room with name 'room_bestaat_niet' does not exist!
```

# Leave Room
### Happy Flow
```shell script
c: LEAVE
r: 205 Quentin left my_first_room
```

### Sad Flow
Als je niet in een room zit en je probeert deze te verlaten:
```shell script
c: LEAVE
s: 404 You're not in a room
```

# Talk in Room
Om in een room te praten gebruik je het `TALK` commando.

### Happy Flow

```shell script
c: TALK Hello World!
r: TALK Quentin Hello World!
```

### Sad Flow
Geen bericht meegegeven.
```shell script
c: TALK
s: 409 Please give a message to send
```

Geen room gejoind.
```shell script
c: TALK Hallo
s: 404 You haven't joined a room to talk in
```

# Gebruikers in een room
Je kunt alle gebruikers van een room krijgen door het `ROOM` commando te gebruiken.

### Happy Flow
In dit geval zitten wij in `my_first_room`.
Net als in [ROOMS](#list-rooms) zijn de namen gescheiden met een punt-comma.
```shell script
c: ROOM
s: ROOM Quentin;Joost
 ```

### Sad Flow
Nu zitten we niet in een room.
```shell script
c: ROOM
s: 404 Join a room first
```

# Emergency Meeting (Vote Kicking)
Als je een meeting start, kun je stemmen op iemand die in de groep zit.
Je kunt ook op jezelf stemmen.

### Happy Flow
In dit geval zitten wij weer in `my_first_room`.
```shell script
c: KICK
r: KICK Vote kick started
```

Na 30 seconden stopt de kick automatisch en wordt degene met de meeste stemmen gekicked.
Of niemand als de stemmen gelijk zijn.
```shell script
r: 202 No one was kicked
```

### Sad Flow
Er is al een emergency meeting gestart.
```shell script
c: KICK
s: 407 A kick has already been requested
```

Je zit niet in een room.
```shell script
c: KICK
s: 404 Join a room first
```

# Vote voor een Gebruiker
### Happy Flow
In dit geval zitten wij weer in `my_first_room`.
De gebruikers in deze room zijn Quentin en Joost.
```shell script
c: VOTE Joost
s: 207 Joost 1
```

### Sad Flow
Niet in een room.
```shell script
c: VOTE Joost
```

