# ARES P2P Protocol | Distributed Software

![Java](https://img.shields.io/badge/language-Java-orange.svg)
![Academic](https://img.shields.io/badge/Academic-University%20Project-blue.svg)
![Status](https://img.shields.io/badge/status-Completed-green.svg)

> **Academic Disclaimer:** This repository contains a university project developed for the Distributed Software / Distributed Systems course. It is an educational proof-of-concept designed to demonstrate the practical implementation of network protocols, socket programming, and concurrent architectures in Java. 

## Overview

Implementation of a centralized Peer-to-Peer (P2P) file-sharing system based on the **ARES** protocol. This distributed system enables the interconnection of multiple nodes (Clients) through a central routing node (Server) that handles registration, file indexing, and transfer triangulation without storing the files itself.

## Key Technical Features

- **Multithreaded Architecture:** The server efficiently handles simultaneous connections using independent threads (`ClientHandler`), avoiding bottlenecks in a distributed environment.
- **Asynchronous Client:** Utilizes a background `ServerListener` thread to keep network listening active without blocking user interaction in the console.
- **Efficient File Transfer:** Reads and writes files dynamically in 8192-byte chunks. This prevents RAM overflow and system crashes during large file transfers between peers.
- **Robust Protocol Design:** Full implementation of custom control Opcodes (e.g., `SEARCH_REQUEST`, `CHUNK_RESPONSE`), including a graceful disconnect system (`0x0F`) to free up server resources safely.
- **Thread-Safety:** Uses concurrent collections (`ConcurrentHashMap`) for the safe management of the global file and user registry across multiple threads.

## Technologies Used

- **Language:** Java 17+
- **Network Communication:** TCP/IP Sockets
- **Data Structures:** ConcurrentHashMap, Byte Buffers, I/O Streams
- **Architecture:** Centralized P2P (Tracker-Peer model)

## Credits
- **Original Assignment Repository:** [SoftwareDistribuitUB-2026/Enunciat-PR1](https://github.com/SoftwareDistribuitUB-2026/Enunciat-PR1)
Universitat de Barcelona
