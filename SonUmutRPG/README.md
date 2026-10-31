# SonUmutRPG

Tek jar, modüler Türkçe RPG eklentisi. **Paper 1.21.4 • Java 21 • Maven**.

## Derleme

```
mvn clean package
```
Çıktı: `target/SonUmutRPG.jar` → sunucunun `plugins/` klasörüne at.

> ⚠️ **Bu kod yazıldığı ortamda derlenip test edilemedi** (internet kapalıydı, Paper API indirilemedi).
> İlk `mvn package`'ta küçük derleme hataları çıkabilir; hata çıktısını yapıştırırsan düzeltilir.
> Özellikle şüpheli tek nokta: `hud/HudModule.java` içindeki `blankNumbers(...)` (Paper `NumberFormat.blank()` API'si).
> Hata verirse yalnızca o metodun gövdesini silmen yeterli (sağdaki kırmızı sayılar görünür kalır).

## Modüller (`config.yml` → `modules:`)

| id | Ne yapar |
|---|---|
| rpg | Sınıf (5), seviye, XP, yetenek puanı, mana, kritik/kaçınma/can çalma/yansıtma, sınıf yetenekleri (Shift+F), sınıf değiştirme kartı |
| moblevel | Düşman mobları çevredeki oyuncuların seviye ortalamasına göre seviyelenir (tüm dünyalar) |
| loot | Mob bazlı ganimet tabloları (`mobloot.yml`) + yönetici GUI |
| forge | Silah/zırh +1…+10 geliştirme (`/demirci`) |
| crate | Anahtarlı kutular, kayan şerit animasyonu, önizleme, yönetici GUI |
| quest | KILL / MINE / BOSS görevleri (`/gorev`), yönetici GUI |
| boss | Can çubuğu, fazlar, yer sarsıntısı, hasara göre ödül, yeniden doğma |
| trade | Güvenli oyuncu takası (`/takas`) |
| megaphone | Tek kullanımlık sunucu duyurusu eşyası (`/megafon`) |
| party | Parti, XP paylaşımı, parti sohbeti (`/parti`, `/p`) |
| clan | Klan, rütbeler, banka, klan sohbeti (`/klan`, `/kc`) |
| hud | Kenar çubuğu, tab listesi, animasyonlu başlık, actionbar |
| chat | Sohbet biçimi, hover kartı, @etiket, `&` renk (yetkiyle) |

## Oyuncu komutları

`/sinif` `/profil` `/gorev` `/demirci` `/takas <oyuncu|kabul|red|altin n>` `/parti ...` `/p <mesaj>` `/klan ...` `/kc <mesaj>` `/megafon al`

Sınıf yeteneği: **Shift + F** (mana harcar, bekleme süresi var).

## Yönetici komutları (`sonumut.admin` ya da `sonumut.<modül>.admin`)

- `/sonumut reload` — config + messages.yml yeniden yükle
- `/sonumut modul <id> ac|kapat` — modülü canlı aç/kapat
- `/sonumut debug`
- `/sonumut ver <tür> <oyuncu> [adet] [ekstra]` — türler: `sinifkarti`, `megafon`, `anahtar <kutuId>`
- `/sonumut loot` — ganimet tabloları GUI
- `/sonumut kutu` — kutu GUI • `/sonumut kutu blok <id>` (baktığın bloğu kutuya bağla/çöz) • `/sonumut kutu sil <id>`
- `/sonumut gorev` — görev GUI
- `/sonumut boss` — boss GUI • `/sonumut boss spawn|drop|respawn <id> ...`

GUI'lerde sohbetten girdi istenir; **"iptal"** yazınca vazgeçilir (30 sn zaman aşımı).

## Dosyalar

```
plugins/SonUmutRPG/
  config.yml        ayarlar
  messages.yml      tüm metinler (MiniMessage) — kullanıldıkça kendiliğinden dolar
  mobloot.yml  crates.yml  quests.yml  bosses.yml  clans.yml
  data/players/<uuid>.yml   data/economy.yml
  logs/             forge, crate, trade, clan, boss, quest, megaphone .log
```
Tüm yazımlar tek bir arka plan iş parçacığından, atomik (geçici dosya + taşıma) yapılır.

## Bilinçli varsayımlar / sınırlar

- **Altın ekonomisi eklentinin kendi içindedir** (`data/economy.yml`). Vault köprüsü yok. Altını vermek için `EconomyService` kullan.
- `messages.yml`'ye varsayılan metinler çağrı anında yazılır; dosya ilk açılışta boştur, oyunda kullanıldıkça dolar.
- `CreatureSpawnEvent` nedeni `CUSTOM` olan mobler (başka eklentiler) seviyelenmez; spawner mobları XP/Altın/ganimet vermez (farm engeli).
- Takasta eşyalar menü envanterinde durur; sunucu çökerse o anki takas eşyaları kaybolabilir.
- Sınıf yeteneği tuşu Shift+F'dir (yalnızca eğilirken el değiştirme iptal edilir).
- Profil önbelleği çevrimdışı sorgulanan oyuncular için sunucu kapanana kadar bellekte kalır (küçük).
- Hiçbir sınıf/menü yapısı derleme ile doğrulanmadı (yukarıdaki uyarı).
