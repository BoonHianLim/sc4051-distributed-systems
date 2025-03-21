from typing import Any, overload, Literal


@overload
def safe_input(
    prompt: str, type: Literal["int"], confirmation: bool = ...) -> int: ...


@overload
def safe_input(
    prompt: str, type: Literal["str"],  confirmation: bool = ...) -> str: ...


def safe_input(prompt: str, type: Literal["int", "str"], confirmation=True, **kwargs: Any) -> int | str:
    if type == "int":
        return _input_int(prompt, confirmation, **kwargs)
    elif type == "str":
        return _input_str(prompt, confirmation, **kwargs)
    else:
        raise ValueError("Invalid type")


def _input_int(prompt: str, confirmation: bool = True, **kwargs: Any) -> int:
    while True:
        try:
            user_input = input(prompt)
            if confirmation:
                print("You have entered:", user_input)
            if max_val := kwargs.get("max_val"):
                if int(user_input) > max_val:
                    raise ValueError(
                        f"Input must be less than or equal to {max_val}")
            if min_val := kwargs.get("min_val"):
                if int(user_input) < min_val:
                    raise ValueError(
                        f"Input must be greater than or equal to {min_val}")
            return int(user_input)
        except KeyboardInterrupt:
            print("Goodbye!")
            exit()
        except ValueError as e:
            print(e)
            print("Invalid input. Please try again.")
        finally:
            print()


def _input_str(prompt: str, confirmation: bool = True, **kwargs: Any) -> str:
    user_input = input(prompt)
    if confirmation:
        print("You have entered:", user_input)
    print()
    return user_input
