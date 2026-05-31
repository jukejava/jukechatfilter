# JukeChatFilter

> Advanced chat filter solution originally made for jUkESMP - now public.

JukeChatFilter gives server staff full control over chat moderation. Filter bad words across chat, commands, signs, books, and anvils - with real-time staff alerts, anti-spam, violation tracking, and automated punishments.

---

## Features

- **Multi-module filtering** - Covers chat, commands, signs, books, and anvils
- **Blocked & whitelisted words** - Full control over what's blocked, with smart whitelisting (e.g. "glass" won't trigger "ass")
- **Anti-spam** - Configurable cooldown between messages
- **Violation tracking** - Persistent violation counts with threshold-based automated punishments
- **Real-time staff alerts** - Notify online staff instantly when a filter is triggered, with word and message context
- **Chat history** - View a log of bad chat via `/chathistory`
- **Fully configurable** - Messages, sounds, punishments, and modules all customizable

---

## Commands & Permissions

| Command | Alias | Description | Permission |
|---|---|---|---|
| `/jukechatfilter reload` | `/jcf reload` | Reload the plugin configuration | `jukechatfilter.admin` |
| `/jukechatfilter alerts` | `/jcf alerts` | Toggle real-time staff alerts on/off | `jukechatfilter.alerts` |
| `/chathistory` | - | View history of blocked messages | `jukechatfilter.admin` |

> All permissions default to **OP**.

---

## Installation

1. Download the latest `JukeChatFilter.jar` from the [Releases](../../releases) page
2. Place the `.jar` into your server's `/plugins` folder
3. Restart your server
4. Configure the plugin in `/plugins/JukeChatFilter/config.yml`

**Requirements:** Paper/Spigot 1.21+

---

## Configuration

### config.yml

```yaml
enabled: true

modules:
  chat:
    enabled: true
  commands:
    enabled: true
    monitored-commands:
      - "msg"
      - "tell"
      - "me"
      - "w"
      - "r"
  signs:
    enabled: true
  books:
    enabled: true
  anvils:
    enabled: true

anti-spam:
  enabled: true
  cooldown-ms: 1000

blocked-words:
  - "badword1"
  - "badword2"

whitelisted-words:
  - "assassin"
  - "glass"

punishments:
  persistent-violations: true
  thresholds:
    15: "punish %player% chat-abuse"
    30: "punish %player% chat-abuse"
    50: "punish %player% chat-abuse"
```

### Placeholders

| Placeholder | Description |
|---|---|
| `%player%` | The offending player's name |
| `%word%` | The word that triggered the filter |
| `%type%` | The filter type (chat, command, sign, etc.) |
| `%message%` | The full message context |
| `%time%` | Remaining cooldown time in seconds |

---

## How It Works

1. A player sends a message, command, or places a sign/book/anvil text
2. JukeChatFilter scans the content against the blocked words list
3. If triggered, the message is **blocked** and the player receives a warning
4. **Staff with alerts enabled** receive an instant notification showing the player, word, type, and full context
5. Each violation increments the player's **violation count**
6. When a violation threshold is reached, a **console command is automatically executed** (e.g. a punishment)

---

## Author

Made by **Juke** - owner & developer at jUkESMP.

- Website: [jukesmp.netlify.app](https://jukesmp.netlify.app)
- GitHub: [jukejava](https://github.com/jukejava)

---

## License

This project is licensed under the [MIT License](LICENSE).
