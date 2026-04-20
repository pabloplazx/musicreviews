"""
Script para poblar la BD con datos de ejemplo.
Crea usuarios, reseñas y favoritos usando la API REST.
Ejecutar desde cualquier directorio: python database/seed_data.py
"""

import urllib.request
import urllib.error
import json

BASE_URL = "http://localhost:8080"

def post(path, body, token=None):
    data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(f"{BASE_URL}{path}", data=data, method="POST")
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req) as r:
            return json.loads(r.read()), r.status
    except urllib.error.HTTPError as e:
        body = e.read()
        try:
            return json.loads(body), e.code
        except Exception:
            return {"mensaje": body.decode("utf-8", errors="replace")}, e.code

def registrar(username, email, password):
    body = {"username": username, "email": email, "password": password}
    resp, status = post("/api/auth/register", body)
    if status == 200:
        print(f"  ✓ Registrado: {username} (id={resp['id']})")
        return resp["token"], resp["id"]
    else:
        # Ya existe — hacer login
        resp, status = post("/api/auth/login", {"email": email, "password": password})
        if status == 200:
            print(f"  ~ Ya existe: {username} (id={resp['id']})")
            return resp["token"], resp["id"]
        print(f"  ✗ Error con {username}: {resp}")
        return None, None

def crear_resena(token, usuario_id, album_id, puntuacion, comentario):
    body = {
        "usuario": {"id": usuario_id},
        "album": {"id": album_id},
        "puntuacion": puntuacion,
        "comentario": comentario
    }
    resp, status = post("/api/resenas", body, token)
    if status == 200:
        print(f"    ✓ Reseña: album {album_id} → {puntuacion}★")
    else:
        print(f"    ~ Reseña album {album_id} ya existe o error: {resp.get('mensaje', resp)}")

def agregar_favorito(token, usuario_id, album_id):
    body = {"usuario": {"id": usuario_id}, "album": {"id": album_id}}
    resp, status = post("/api/favoritos", body, token)
    if status == 200:
        print(f"    ✓ Favorito: album {album_id}")
    else:
        print(f"    ~ Favorito album {album_id} ya existe")


# ── Usuarios ──────────────────────────────────────────────────────────────────

print("\n=== Creando usuarios ===")

usuarios = [
    ("maria_indie",   "maria@musicreviews.com",   "maria123"),
    ("carlos_rap",    "carlos@musicreviews.com",  "carlos123"),
    ("ana_electronica","ana@musicreviews.com",    "ana123"),
    ("jorge_clasicos","jorge@musicreviews.com",   "jorge123"),
    ("lucia_urban",   "lucia@musicreviews.com",   "lucia123"),
]

tokens = {}
ids = {}
for username, email, password in usuarios:
    token, uid = registrar(username, email, password)
    if token:
        tokens[username] = token
        ids[username] = uid


# ── Reseñas ───────────────────────────────────────────────────────────────────
# Álbumes usados:
#   Radiohead:       7=A Moon Shaped Pool, 5=KID A MNESIA
#   The Beatles:     13=Let It Be Naked, 9=Anthology 4
#   Kanye West:      50=Donda (Deluxe), 78=CHROMAKOPIA (Tyler)
#   Tyler Creator:   78=CHROMAKOPIA, 80=CALL ME IF YOU GET LOST
#   ROSALÍA:         84=El Mal Querer, 85=Los Ángeles
#   Pink Floyd:      94=Live at Knebworth, 92=Pompeii
#   Kendrick Lamar:  110=DAMN., 108=Black Panther
#   Daft Punk:       73=Random Access Memories, 72=RAM 10th Anniversary
#   The Smiths:      125=Meat Is Murder, 122=Strangeways
#   The Strokes:     64=First Impressions Of Earth, 63=Angles
#   Jay-Z:           220=4:44
#   The Weeknd:      133=Dawn FM
#   Bad Bunny:       id a buscar, usamos Oasis: 253=Definitely Maybe

print("\n=== Creando reseñas ===")

# maria_indie — fan del indie y rock alternativo
if "maria_indie" in tokens:
    print("\n  maria_indie:")
    t, uid = tokens["maria_indie"], ids["maria_indie"]
    crear_resena(t, uid, 7,   5, "Una obra maestra. Thom Yorke en estado puro, cada canción es un viaje emocional. 'True Love Waits' me dejó sin palabras.")
    crear_resena(t, uid, 5,   5, "KID A y Amnesiac juntos es demasiado para el corazón. El mejor doble álbum de la historia del rock.")
    crear_resena(t, uid, 122, 4, "The Smiths en su punto más oscuro y poético. Morrissey nunca estuvo tan inspirado.")
    crear_resena(t, uid, 125, 5, "Crudeza y belleza en partes iguales. 'How Soon Is Now?' debería enseñarse en las escuelas.")
    crear_resena(t, uid, 64,  4, "'Juicebox', 'Heart In A Cage'... The Strokes demostraron que podían crecer sin perder su esencia.")
    crear_resena(t, uid, 253, 5, "Definitivamente Oasis en su mejor momento. '\"Champagne Supernova\"' y 'Wonderwall' son himnos generacionales.")

# carlos_rap — fan del hip hop
if "carlos_rap" in tokens:
    print("\n  carlos_rap:")
    t, uid = tokens["carlos_rap"], ids["carlos_rap"]
    crear_resena(t, uid, 110, 5, "DAMN. es un ejercicio perfecto de rap introspectivo. 'HUMBLE.' y 'DNA.' son tracks que no envejecen.")
    crear_resena(t, uid, 108, 4, "La BSO de Black Panther como álbum independiente funciona muy bien. Kendrick lo eleva todo.")
    crear_resena(t, uid, 50,  4, "Donda tiene momentos brutales entre tanto ruido. 'Jail', 'Hurricane', 'Jail pt 2' son de lo mejor de Kanye en años.")
    crear_resena(t, uid, 220, 5, "4:44 es el Jay-Z más maduro y honesto. Hablar de infidelidad, dinero y legado con esa honestidad es valiente.")
    crear_resena(t, uid, 80,  5, "CALL ME IF YOU GET LOST es Tyler en modo genio. El concept album más creativo desde IGOR.")
    crear_resena(t, uid, 78,  4, "CHROMAKOPIA tiene momentos increíbles. Tyler sigue evolucionando sin parar.")

# ana_electronica — fan de la electrónica y lo experimental
if "ana_electronica" in tokens:
    print("\n  ana_electronica:")
    t, uid = tokens["ana_electronica"], ids["ana_electronica"]
    crear_resena(t, uid, 73,  5, "Random Access Memories es el álbum más ambicioso de Daft Punk. 'Get Lucky' y 'Giorgio by Moroder' son perfectos.")
    crear_resena(t, uid, 72,  5, "La edición del 10 aniversario añade capas que no había escuchado antes. Una joya atemporal.")
    crear_resena(t, uid, 84,  5, "El Mal Querer redefinió la música española. ROSALÍA construyó algo completamente único mezclando flamenco y producción moderna.")
    crear_resena(t, uid, 85,  4, "Los Ángeles muestra los orígenes de ROSALÍA. Más desnudo, más flamenco puro. Imprescindible para entender su evolución.")
    crear_resena(t, uid, 7,   4, "A Moon Shaped Pool tiene una belleza fría y melancólica que engancha. Radiohead siempre sorprendiendo.")
    crear_resena(t, uid, 133, 5, "Dawn FM es The Weeknd en modo conceptual. La idea de la radio en el purgatorio es brillante.")

# jorge_clasicos — fan del rock clásico
if "jorge_clasicos" in tokens:
    print("\n  jorge_clasicos:")
    t, uid = tokens["jorge_clasicos"], ids["jorge_clasicos"]
    crear_resena(t, uid, 13,  4, "Let It Be Naked elimina la producción de Spector y deja a los Beatles tal como eran. Más honesto.")
    crear_resena(t, uid, 9,   3, "El Anthology tiene valor histórico enorme pero como escucha continua se hace largo. Para fans dedicados.")
    crear_resena(t, uid, 94,  5, "Pink Floyd en directo en Knebworth es historia del rock. 'Comfortably Numb' en vivo hace llorar.")
    crear_resena(t, uid, 253, 5, "Definitely Maybe es uno de los mejores debuts del rock. Cada canción es un himno.")
    crear_resena(t, uid, 122, 4, "Strangeways es el testamento de The Smiths. Triste que fuera el último.")
    crear_resena(t, uid, 64,  5, "First Impressions of Earth es el álbum más ambicioso de The Strokes. No todos lo apreciaron, pero es magnífico.")

# lucia_urban — fan del urbano español y latino
if "lucia_urban" in tokens:
    print("\n  lucia_urban:")
    t, uid = tokens["lucia_urban"], ids["lucia_urban"]
    crear_resena(t, uid, 84,  5, "El Mal Querer es pura obra de arte. Cada escucha descubres algo nuevo. ROSALÍA es un genio.")
    crear_resena(t, uid, 85,  4, "Los Ángeles es donde todo empezó. Increíble para ser un primer álbum.")
    crear_resena(t, uid, 133, 4, "Dawn FM tiene un rollo muy ochentero que me encanta. 'Sacrifice' es un banger.")
    crear_resena(t, uid, 110, 4, "DAMN. lo escucho en bucle desde que salió. Kendrick es de otro nivel.")
    crear_resena(t, uid, 78,  5, "CHROMAKOPIA es el álbum del año. Tyler siempre está diez pasos por delante.")
    crear_resena(t, uid, 80,  4, "CALL ME IF YOU GET LOST tiene un flow increíble de principio a fin.")


# ── Favoritos ─────────────────────────────────────────────────────────────────

print("\n=== Añadiendo favoritos ===")

if "maria_indie" in tokens:
    print("\n  maria_indie:")
    t, uid = tokens["maria_indie"], ids["maria_indie"]
    for album_id in [7, 5, 122, 125, 64, 253]:
        agregar_favorito(t, uid, album_id)

if "carlos_rap" in tokens:
    print("\n  carlos_rap:")
    t, uid = tokens["carlos_rap"], ids["carlos_rap"]
    for album_id in [110, 220, 80, 78, 50]:
        agregar_favorito(t, uid, album_id)

if "ana_electronica" in tokens:
    print("\n  ana_electronica:")
    t, uid = tokens["ana_electronica"], ids["ana_electronica"]
    for album_id in [73, 72, 84, 85, 133]:
        agregar_favorito(t, uid, album_id)

if "jorge_clasicos" in tokens:
    print("\n  jorge_clasicos:")
    t, uid = tokens["jorge_clasicos"], ids["jorge_clasicos"]
    for album_id in [94, 253, 13, 122, 64]:
        agregar_favorito(t, uid, album_id)

if "lucia_urban" in tokens:
    print("\n  lucia_urban:")
    t, uid = tokens["lucia_urban"], ids["lucia_urban"]
    for album_id in [84, 85, 133, 78, 110]:
        agregar_favorito(t, uid, album_id)

print("\n=== ¡Datos de ejemplo cargados! ===\n")
