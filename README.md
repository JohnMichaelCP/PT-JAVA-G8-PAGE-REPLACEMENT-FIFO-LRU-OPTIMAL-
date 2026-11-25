# PT-JAVA-G8-PAGE-REPLACEMENT-FIFO-LRU-OPTIMAL

## Overview
This project is a Java-based Page Replacement Simulator that demonstrates and compares three page replacement algorithms used in Operating Systems: **First-In, First-Out (FIFO), Least Recently Used (LRU), and Optimal Page Replacement**.

The simulator illustrates how an operating system manages pages in memory when a page is requested and the available memory frames are limited. When a requested page is not currently loaded into memory, a page fault occurs. If all frames are occupied, the selected page replacement algorithm determines which page should be removed to make room for the new page.

The project provides a practical way to understand the behavior, execution, and performance of different page replacement algorithms through page references, memory frames, and page fault calculations.

## Objectives
- Understand the concept of virtual memory and page replacement.
- Demonstrate how FIFO, LRU, and Optimal Page Replacement algorithms work.
- Simulate page references using a fixed number of memory frames.
- Identify page hits and page faults during execution.
- Compare the number of page faults produced by each algorithm.
- Develop a better understanding of memory management in operating systems.

## Page Replacement Algorithms

### 1. First-In, First-Out (FIFO)
FIFO replaces the page that has been in memory for the longest time. It follows the order in which pages were loaded into the frames.

### 2. Least Recently Used (LRU)
LRU replaces the page that has not been used for the longest period of time. It considers the recent usage history of pages.

### 3. Optimal Page Replacement
Optimal Page Replacement replaces the page that will not be used for the longest time in the future. It is used as a theoretical benchmark for comparing the performance of other page replacement algorithms.

## Features
- Simulates page replacement using FIFO, LRU, and Optimal algorithms.
- Uses a fixed number of memory frames.
- Processes a sequence of page references.
- Detects page hits and page faults.
- Displays the page replacement process.
- Calculates the total number of page faults.
- Allows comparison of the three algorithms.

## Technologies Used
- Java
- Object-Oriented Programming
- Data Structures
- Operating Systems Concepts

## Project Purpose
This project was developed as an academic programming project to apply Java programming concepts and understand the implementation of page replacement algorithms in operating systems.
It demonstrates how different page replacement strategies affect memory utilization and page fault frequency when processing the same sequence of page references.
