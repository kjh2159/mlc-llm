"""Operators for pipeline parallelism."""

from typing import List

from tvm import relax, tir
from tvm.relax.frontend.nn import Tensor, op


def pipeline_stage_boundary(*tensors: Tensor) -> List[Tensor]:
    """Pipeline parallelism stage boundary mark operator in MLC.

    Parameters
    ----------
    tensors : Tensor
        The tensors to be passed to the next stage.

    Returns
    -------
    tensors : List[Tensor]
        The list of input tensors passed to the next stage.
    """
    # pylint: disable=protected-access
    return op.wrap_nested(
        relax.call_pure_packed(
            "mlc.pipeline_parallel_stage_boundary",
            *[tensor._expr for tensor in tensors],
            sinfo_args=(
                tensors[0]._expr.struct_info
                if len(tensors) == 1
                else relax.TupleStructInfo([tensor._expr.struct_info for tensor in tensors])
            ),
        ),
        name="pipeline_stage_boundary",
    )
    # pylint: enable=protected-access

def layer_pause(x: Tensor, layer_id: int = -1, point_id: int = -1) -> Tensor:
    """Layer-wise pause mark operator in MLC.

    Parameters
    ----------
    x : Tensor
        The input tensor to be marked.
    layer_id : int
        The layer id of the pause point.
    point_id : int
        The point id of the pause point (e.g., 0 for attention exit, 1 for MLP exit).

    Returns
    -------
    Tensor
        The output tensor with the same content as input, but with a side effect of marking the layer pause point.
    """
    # pylint: disable=protected-access
    return op.wrap_nested(
        relax.call_pure_packed(
            "mlc.debug.layer_pause",
            x._expr,
            tir.IntImm("int32", layer_id),
            tir.IntImm("int32", point_id),
            sinfo_args=x._expr.struct_info,
        ),
        name="layer_pause",
    )
    # pylint: enable=protected-access
