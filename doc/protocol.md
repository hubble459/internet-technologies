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

#### Status codes:
- 2xx = Positief
- 4xx = Error

#### Response:
Alle responses van de server op een commando is een Status Code.

Alle mededelingen zijn woorden

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

### Happy Flow TODO
```shell script
c: BCST Hello World
s: 200 Message send
e: BCST Quentin Hello World
```

### Sad Flow
Geen bericht
```shell script
c: BCST
s: 409 Please give a message to send
```

# Maak een Groep
Maak een room met het `MAKE` commando.

### Happy Flow
```shell script
c: MAKE my_first_room
e: MADE my_first_room
```

### Sad Flow
```shell script
c: MAKE [name that does not match requirements or no name]
s: 402 Room name should be between 3 and 14 characters and should match [a-zA-Z_0-9]
```

```shell script
c: MAKE [with name that already exists]
s: 405 Room with name room_bestaat_al already exists
```

# List Rooms
### Happy Flow
De room namen zijn gescheiden door punt-comma's,
daarom mag je geen room aanmaken met een punt-comma in de naam.
# todo 200 response
```shell script
c: ROOMS [should be empty, but doesnt matter]
s: 200 owo;swag;shrek;my_first_room
```
Wanneer er geen rooms zijn wordt er een lege 200 terug gestuurd:
```shell script
c: ROOMS
s: 200
```

### Sad Flows
There's nothing sad about rooms

# Join Room
Als je al in een room zit verlaat je deze automatisch.
Zie [leave room](#leave-room) voor details.

### Happy Flow
```shell script
c: JOIN my_first_room
r: JOINED Quentin joined my_first_room
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
r: LEFT Quentin left my_first_room
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
s: 200
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

# Gebruikers in een Room
Je kunt alle gebruikers van een room krijgen door het `ROOM` commando te gebruiken.

### Happy Flow
In dit geval zitten wij in `my_first_room`.
Net als in [ROOMS](#list-rooms) zijn de namen gescheiden met een punt-comma.
```shell script
c: ROOM
s: 200 Quentin;Joost
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
s:
r: KICK Vote kick started
```

Na 30 seconden stopt de kick automatisch en wordt degene met de meeste stemmen gekicked.
Of niemand als de stemmen gelijk zijn.
```shell script
r: KRES 0 No one was kicked
of
r: KRES 1 [naam]
```

Als iedereen ... ook.

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
s: VOTES Joost 1; Quentin 0
# Nu ingelogt als Joost
c: VOTE Quentin
r: VOTES Joost 1; Quentin 1
r: KRES 0 No one was kicked
```

```shell script
c: VOTE Joost
r: VOTES Joost 1; Quentin 0
# Nu ingelogt als Joost
c: VOTE Joost
r: VOTES Joost 2; Quentin 0
r: KRES 1 Joost was kicked ówò
```

### Sad Flow
Niet in een room.
```shell script
c: VOTE Joost
s: 404 Join a room first
```

Vote persoon wie niet in de room zit.
```
c: VOTE unknown_user
s: 400 No user with this username found
```