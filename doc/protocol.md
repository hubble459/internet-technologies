# Opzetten Verbinding

Bij het verbinden met de server op poort 1337 krijg je een `INFO Welcome` bericht terug.

Na verbinden moet je inloggen.

Anders krijg je de error:

```
s: 400 Please login first
```

- c = Client to server
- s = Server to you
- e = Server to Everyone
- o = Server to everyone but you (Others)
- r = Server to everyone in the Room

#### Status codes:

- 200 = Positief
- 400 = Error

#### Response:

Alle responses van de server op een commando is een Status Code.

Alle mededelingen zijn woorden

# Login

Om in te loggen moet je een `CONN [username] [public key]` bericht sturen.

### Happy Flow

Ingelogd als Quentin; wordt in de andere flows gebruikt als de ingelogde user.

```
c: CONN Quentin abcd==
s: 200 Logged in as Quentin
o: JSERVER Quentin joined the server
```

### Sad Flow

```
c: CONN [name that is already in use]
s: 400 User with username 'Quentin' is already logged in
```

```
c: CONN [name that does not match requirements]
s: 400 Name should be between 3 and 14 characters and should match [a-zA-Z_0-9]
```

# Bericht Versturen

Als je een bericht naar iedereen op de server wilt versturen dan gebruik je `BCST`.

### Happy Flow TODO

```
c: BCST Hello World
s: 200 Message send
e: BCST Quentin Hello World
```

### Sad Flow

Geen bericht

```
c: BCST
s: 400 Please give a message to send
```

# Maak een Groep

*Mogelijkheid voor het aanmaken van een groep.*

Maak een room met het `MAKE` commando.

### Happy Flow

```
c: MAKE my_first_room
s: 200 my_first_room
e: MADE my_first_room
```

### Sad Flow

```
c: MAKE [name that does not match requirements or no name]
s: 400 Room name should be between 3 and 14 characters and should match [a-zA-Z_0-9]
```

```
c: MAKE [with name that already exists]
s: 400 Room with name room_bestaat_al already exists
```

# List Users

*Lijst van verbonden gebruikers opvragen door de client.*

De namen zijn gescheiden door punt-comma's, daarom mag je niet inloggen met een punt-comma in de naam.

### Happy Flow

```
c: USERS
s: 200 Quentin;Joost
```

# List Rooms

*Een lijst van groepen opvragen.*

### Happy Flow

De room namen zijn gescheiden door punt-comma's, daarom mag je geen room aanmaken met een punt-comma in de naam.

```
c: ROOMS [should be empty, but doesnt matter]
s: 200 owo;swag;shrek;my_first_room
```

Wanneer er geen rooms zijn, wordt er een lege response terug gestuurd:

```
c: ROOMS
s: 200
```

### Sad Flows

There's nothing sad about rooms

# Join Room

*Deelnemen aan een groep.*

Als je al in een room zit verlaat je deze automatisch. Zie [leave room](#leave-room) voor details.

### Happy Flow

```
c: JOIN my_first_room
s: 200 my_first_room
r: JROOM Quentin joined my_first_room
```

### Sad Flow

```
c: JOIN room_bestaat_niet
s: 400 Room with name 'room_bestaat_niet' does not exist!
```

# Leave Room

*Uit een groep stappen.*

### Happy Flow

```
c: LEAVE
s: 200 my_first_room
r: RLEFT Quentin left my_first_room
```

### Sad Flow

Als je niet in een room zit en je probeert deze te verlaten:

```
c: LEAVE
s: 400 You're not in a room
```

# Talk in Room

*Een bericht te sturen naar alle deelnemers van een groep.*

Om in een room te praten gebruik je het `TALK` commando.

### Happy Flow

```
c: TALK Hello World!
s: 200 Hello World!
r: TALK Quentin Hello World!
```

### Sad Flow

Geen bericht meegegeven.

```
c: TALK
s: 400 Please give a message to send
```

Geen room gejoind.

```
c: TALK Hallo
s: 400 You haven't joined a room to talk in
```

# Gebruikers in een Room

Je kunt alle gebruikers van een room krijgen door het `ROOM` commando te gebruiken.

### Happy Flow

In dit geval zitten wij in `my_first_room`. Net als in [ROOMS](#list-rooms) zijn de namen gescheiden met een punt-comma.

```
c: ROOM
s: 200 Quentin;Joost
 ```

### Sad Flow

Nu zitten we niet in een room.

```
c: ROOM
s: 400 Join a room first
```

# Emergency Meeting

*Mogelijkheid om een gebruiker uit een groep te kicken.*

Als je een meeting start, kun je stemmen op iemand die in de groep zit. Je kunt ook op jezelf stemmen.

### Happy Flow

In dit geval zitten wij weer in `my_first_room`.

```
c: KICK
s: 200 my_first_room
r: KICK Vote kick started
```

Na 30 seconden en niet iedereen heeft gevote stopt de kick automatisch en wordt degene met de meeste stemmen gekicked.
Of niemand als de stemmen gelijk zijn. Dit gebeurt ook als iedereen in de room heeft gestemd.

```
r: KRES 0 No one was kicked
of
r: KRES 1 [naam]
```

### Sad Flow

Er is al een emergency meeting gestart.

```
c: KICK
s: 400 A kick has already been requested
```

Je zit niet in een room.

```
c: KICK
s: 400 Join a room first
```

# Vote voor een Gebruiker

### Happy Flow

In dit geval zitten wij weer in `my_first_room`. De gebruikers in deze room zijn Quentin en Joost.

```
c: VOTE Joost
s: 200 Joost
r: VOTES Joost 1;Quentin 0
# Nu ingelogt als Joost
c: VOTE Quentin
s: 200 Quentin
r: VOTES Joost 1;Quentin 1
r: KRES 0 No one was kicked
```

```
c: VOTE Joost
s: 200 Joost
r: VOTES Joost 1;Quentin 0
# Nu ingelogt als Joost
c: VOTE Joost
s: 200 Joost
r: VOTES Joost 2;Quentin 0
r: KRES 1 Joost was kicked ówò
```

```
c: SKIP
s: 200
r: VOTES Joost 0;Quentin 0
# Nu ingelogt als Joost
c: VOTE Quentin
s: 200 Quentin
r: VOTES Joost 0;Quentin 1
r: KRES 1 Quentin was kicked ówò
```

### Sad Flow

Niet in een room.

```
c: VOTE Joost
s: 400 Join a room first
```

Vote persoon die niet in de room zit.

```
c: VOTE unknown_user
s: 400 No user with this username found
```

# WHISPER (Private Message / Direct Message)

*Een privé bericht (direct message) sturen naar een andere gebruiker.*

Als je een bericht bericht naar een specifiek iemand wil sturen gebruik je de `SEND` command.

### Happy Flow

Joost stuurt een privé bericht naar Quentin.

```
c: SEND Quentin Hallo
s: 200 Quentin Hallo
(Q's client): SEND Joost Hallo
```

### Sad Flow

Wanneer de gebruikersnaam niet bestaat

```
c: SEND unkonwn Hallo
s: 400 No user with this username found
```

Wanneer er geen bericht of username wordt meegegeven.

```
c: SEND Quentin
s: 400 No username/message given
```

```
c: SEND
s: 400 No username/message given
```

# QUIT

Je kunt de server verlaten door een `QUIT` bericht te sturen.

```
c: QUIT
s: 200 Quit successfully
e: LEFT Quentin left
```

# PING

Server stuurt een `PING` bericht elke 30 seconden om te kijken of je nog actief bent. Als je niet op de PING reageert
met een PONG binnen 3 seconden, dan word je automatisch disconnected.

### Happy Flow

```
s: PING
c: PONG
```

### Sad Flow

```
s: PING
s: DCSN
```

# File Transmission

## FILE [ontvanger] [filename] [base64] [MD5 checksum]

Verstuur een bestand (UPLOAD) naar een andere gebruiker.

### Happy Flow

```
c: FILE Joost itech.zip [base64] [checksum]
s: 200
```

### Sad Flow

```
c: FILE Joost itech.zip [base64]
s: 400 Invalid number of arguments passed

c: FILE non_existent itech.zip [base64] [checksum]
s: 400 User with username 'non_existent' does not exist

c: FILE Joost itech.zip [base64] [wrong_checksum]
s: 400 Uploaded file does not match given checksum

c: FILE Joost itech.zip [base64] [checksum]
s: 400 Failed to write file on server
```

## <NOTIFICATION> FILE [from_username] [filename] [size_in_mb]
Als je een FILE command van server ontvangt dan krijg je een bestand aangeboden.
Om deze te downloaden gebruik je het commando wat hieronder staat.

## DOWN [filename]

Bestand ontvangen van een gebruiker.

### Happy Flow

```
s: FILE Quentin itech.zip 1.2
c: DOWN itech.zip
s: 200 [base64] [checksum]
c: compares checksums
```

### Sad Flow

```
s: FILE Quentin itech.zip 1.2
c: DOWN not_exist.zip
s: 400 Requested file 'not_exist.zip' not found

s: FILE Quentin itech.zip 1.2
c: DOWN not_exist.zip uwu
s: 400 Invalid number of arguments passed

s: FILE Quentin itech.zip 1.2
c: DOWN itech.zip uwu
s: 400 Server could not read file; try again
```
