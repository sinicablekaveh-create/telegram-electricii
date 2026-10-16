# Telegram Electric: current state and architecture

## Current alpha status

The repository is a scaffold. It does not package TDLib's Java/JNI runtime, authenticate a Telegram account, load the account's chats, or deliver messages. The UI disables unavailable actions, and core methods fail closed rather than reporting that an engine started or a message was sent.

## TDLib integration prerequisites

1. Package the official TDLib Java bindings and native libraries for the Android ABIs this app supports.
2. Provide the Telegram `api_id` and `api_hash` through local or CI configuration. Never commit credentials or a user's login code/session.
3. Handle TDLib's asynchronous authorization states and ordered updates, including chat updates, before enabling chat selection.
4. Build the selectable list from chats visible to the signed-in account. Do not scrape group members or join chats automatically.
5. Before outbound messaging, show the exact selected chats and message text and require an explicit confirmation. Start with a user-controlled test chat, honor Telegram limits, and provide a reliable stop control.

## Planned components

- TDLib client lifecycle and authorization
- Chat/update repository backed by the signed-in user's visible chats
- Explicit target selection and message preview
- A rate-limited, stoppable send queue with clear sent/uncertain results
- Electric WordBank as an optional draft helper; generated text never sends automatically

## Roadmap

- Alpha: package and initialize TDLib, complete sign-in, and display the user's chats.
- Beta: implement user-confirmed messaging to selected chats with limits, recovery, and tests.
