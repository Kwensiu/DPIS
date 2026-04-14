# Per-App DPI PoC Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a minimal Android Xposed module PoC that installs a first hook on `ResourcesManager.applyConfigurationToResources(...)` and overrides `densityDpi` for one target app.

**Architecture:** Use a small Android app module with `libxposed/api` as `compileOnly`, install hooks from `onPackageReady()`, and keep density math in plain Java helpers so it can be validated with local unit tests before wiring the hook.

**Tech Stack:** Android Gradle Plugin, Java, libxposed API, JUnit 4

---
