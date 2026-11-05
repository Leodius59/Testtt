# SurveyPilot 0.3 — assistant Android hors ligne

## Fonctionnalités
- Profil personnel stocké localement avec Room / SQLite.
- OCR sur l'appareil à partir d'une capture d'écran choisie dans la galerie (ML Kit Text Recognition, modèle embarqué).
- Le texte reconnu est modifiable avant analyse.
- Recherche locale de faits confirmés et score de correspondance conservateur.
- Suppression individuelle ou complète des données.
- Aucune permission Internet, aucun compte, aucune API, aucun abonnement.
- Aucune réponse inventée et aucune soumission automatique à AttaPoll.

## Limite OCR
Cette version analyse une capture d'écran sélectionnée par l'utilisateur. Elle ne lit pas encore l'écran AttaPoll en continu et ne clique pas à la place de l'utilisateur. Pour analyser une question : faire une capture d'écran dans AttaPoll, ouvrir SurveyPilot, choisir la capture, vérifier le texte extrait, puis lancer la recherche dans le profil.

## Compilation APK
Le dépôt contient un workflow GitHub Actions dans `.github/workflows/build-apk.yml`. Sur GitHub, lancer l'action **Build SurveyPilot APK**, puis télécharger l'artefact `SurveyPilot-debug-apk`.

Configuration : application `fr.surveypilot.app`, minSdk 26, compileSdk 35, Kotlin, Jetpack Compose, Room et ML Kit OCR embarqué. Version 0.3.0.
