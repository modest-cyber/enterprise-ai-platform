"""JSON Schema 校验器"""

import logging

import jsonschema

from app.mcp.exceptions import SchemaValidationError
from app.mcp.models import ValidationResult

logger = logging.getLogger(__name__)


class SchemaValidator:

    def validate(self, schema: dict, params: dict) -> ValidationResult:
        errors: list[str] = []

        try:
            jsonschema.validate(instance=params, schema=schema)
        except jsonschema.ValidationError as e:
            msg = str(e.message)
            if e.schema_path:
                msg = f"{msg} (schema_path={list(e.schema_path)})"
            errors.append(msg)
        except jsonschema.SchemaError as e:
            msg = f"Schema 定义错误: {e.message}"
            errors.append(msg)

        valid = len(errors) == 0

        if not valid:
            logger.warning("Schema 校验失败: %s", errors)
            raise SchemaValidationError("; ".join(errors))

        return ValidationResult(valid=valid, errors=errors)