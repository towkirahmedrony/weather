# Atmos Weather API & Architecture Documentation

## Weather Provider Evaluation

| Provider | Quotas & Pricing | Key Requirements | Capabilities | Decision |
| --- | --- | --- | --- | --- |
| **Open-Meteo** | Free non-commercial access (~10,000 calls/day); commercial plans available | **No API key required** for open tier | 7–16 day forecast, 24h+ hourly, CAPE convective energy, UV, visibility, dew point, surface pressure, Air Quality (`us_aqi`, `european_aqi`, PM2.5, PM10, O₃, NO₂, SO₂, CO), worldwide Geocoding | **Selected Primary Provider** |
| **RainViewer** | Free public radar JSON + tile frames | **No API key required** | Live global precipitation radar tiles | **Selected Map Radar Layer** |
| **WeatherAPI** | 1M calls/mo free, 3-day forecast limit on free plan | Requires private API key | Forecast, astronomy, alerts | Not selected (free tier limits 7-day forecast) |
| **OpenWeather** | 1,000 calls/day One Call 3.0 (requires billing card on file) | Requires private API key | Forecast, map tiles, alerts | Not selected (requires key + card for 7-day One Call) |

## Architecture Overview

- **Provider Abstraction**: `WeatherRepository` isolates all remote APIs from ViewModels and Composables.
- **Offline Caching**: Room (`AtmosDatabase`) caches complete `WeatherSnapshot` JSON payloads (`WeatherCacheEntity`) and `SavedLocationEntity` records, with a 10-minute freshness window and HTTP 429 cooldown handling.
- **Settings & Localization**: DataStore (`PreferencesManager`) + `LocaleHelper` provide persistent Light/Dark/System themes, unit conversions (°C/°F, km/h/m/s/mph/kn, mm/in), and live English (`en`) / Bangla (`bn`) localization.
- **Bangladesh Meteorological Insights**: `BangladeshWeatherAnalyzer` computes NOAA Heat Index, humidity stress, CAPE convective thunderstorm/lightning risk, BMD-calibrated 24h rainfall levels, and gale wind advisories directly from live model telemetry, with explicit attribution to BMD (`live.bmd.gov.bd`) for statutory alerts.
- **Background Sync & Widgets**: `WeatherSyncWorker` (WorkManager) updates Room cache, `CurrentWeatherWidgetProvider` (2×2), `ForecastWeatherWidgetProvider` (4×2), and threshold-based notifications.

## Required Configuration

- No private API keys are required out of the box when using Open-Meteo + RainViewer.
- Any future commercial/private API keys should be placed in AI Studio Secrets (`.env` locally, never committed to GitHub).
- **Attribution**: Weather & Air Quality data by Open-Meteo (CC BY 4.0); Radar tiles by RainViewer; Base map tiles by CartoDB / OpenStreetMap.
