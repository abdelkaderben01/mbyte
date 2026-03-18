# MByte - Setup DNS avec Fichier Hosts

## Objectif
Gérer les entrées DNS des stores en mettant à jour le fichier hosts manuellement ou automatiquement avec `watch-stores.ps1`.

## 1) Démarrer les services (serveur)

```powershell
docker-compose up -d
```

## 2) Configurer le fichier hosts sur le PC client

Le fichier hosts se trouve à : `C:\Windows\System32\drivers\etc\hosts`

Exemple d'entrées à ajouter manuellement :
```
127.0.0.1 www.mbyte.fr
127.0.0.1 sheldon.s.mbyte.fr
127.0.0.1 store-username.s.mbyte.fr
```

## 3) Automatiser les ajouts/suppressions avec watch-stores.ps1

Pour éviter les modifications manuelles à chaque création/suppression de store :

> PowerShell **Admin** requis

```powershell
.\watch-stores.ps1
```

Ce script :
- Surveille les nouveaux stores créés dans les logs Docker
- Ajoute automatiquement les entrées DNS au fichier hosts
- Supprime les entrées DNS quand les stores sont supprimés
- Actualise le cache DNS automatiquement avec `ipconfig /flushdns`

Laissez-le tourner en arrière-plan pendant vos tests.

## 4) Tester

```powershell
ping www.mbyte.fr
```

Vous devriez recevoir une réponse de `127.0.0.1`.

---

## Notes
- Le script `watch-stores.ps1` s'exécute en continu (appuyez sur `Ctrl+C` pour arrêter)
- Les modifications du fichier hosts sont appliquées avec un délai de ~5 secondes
- Vérifiez que vous avez les droits Admin pour modifier le fichier hosts
