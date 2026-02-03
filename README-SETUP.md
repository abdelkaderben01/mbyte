# MByte - Setup DNS Automatique (CoreDNS)

## Objectif
Résoudre automatiquement tous les domaines `*.mbyte.fr` sans modifier le fichier hosts.

## 1) Démarrer les services (serveur)

```powershell
docker-compose up -d
```

Le service DNS s’appelle **mbyte_dns** et utilise le fichier [Corefile](Corefile).

## 2) Configurer le DNS sur chaque PC client (une seule fois)

> PowerShell **Admin** requis

```powershell
.\setup-dns-client.ps1
```

Ce script configure le DNS du PC pour pointer vers le serveur MByte.

## 3) Tester

```powershell
Resolve-DnsName www.mbyte.fr
```

Si OK, tous les domaines `*.mbyte.fr` fonctionneront automatiquement (ex: stores).

---

## Important
Le DNS Windows utilise **le port 53**.
Le serveur CoreDNS doit **écouter sur 53** pour que Windows le prenne comme DNS principal.

Si le port 53 est occupé sur ton serveur Windows :
- soit tu libères le port 53,
- soit tu utilises l’ancienne solution `hosts`.
