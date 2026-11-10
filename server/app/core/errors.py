"""Server error codes exposed to API clients."""

from enum import StrEnum


class ErrorCode(StrEnum):
    IDEMPOTENCY_KEY_CONFLICT = "IDEMPOTENCY_KEY_CONFLICT"
