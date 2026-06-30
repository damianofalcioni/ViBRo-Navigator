package vibro.navigator.brouter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;

// Adapted from BRouter btools.codec.TagValueCoder (MIT) for rd5 dictionary traversal.
final class Rd5TagValueCoder {
    @Nullable
    private final Object tree;
    @NonNull
    private final Rd5BitCoderContext context;
    @NonNull
    private final byte[] tagBuffer = new byte[256];
    @NonNull
    private final Rd5VarBitsWriter tagWriter = new Rd5VarBitsWriter(tagBuffer);

    Rd5TagValueCoder(@NonNull Rd5BitCoderContext context) {
        this.context = context;
        tree = decodeTree();
    }

    @Nullable
    TagValue decodeTagValueSet() {
        Object node = tree;
        while (node instanceof TreeNode) {
            TreeNode treeNode = (TreeNode) node;
            node = context.decodeBit() ? treeNode.child2 : treeNode.child1;
        }
        return (TagValue) node;
    }

    @Nullable
    private Object decodeTree() {
        if (context.decodeBit()) {
            TreeNode node = new TreeNode();
            node.child1 = decodeTree();
            node.child2 = decodeTree();
            return node;
        }
        tagWriter.reset();
        boolean hasData = false;
        for (;;) {
            int delta = context.decodeVarBits();
            if (!hasData && delta == 0) {
                return null;
            }
            if (delta == 0) {
                tagWriter.encodeVarBits(0);
                break;
            }
            hasData = true;
            tagWriter.encodeVarBits(delta);
            tagWriter.encodeVarBits(context.decodeVarBits());
        }
        return new TagValue(tagBuffer, tagWriter.closeAndGetEncodedLength());
    }

    private static final class TreeNode {
        @Nullable
        private Object child1;
        @Nullable
        private Object child2;
    }

    static final class TagValue {
        @NonNull
        private final byte[] data;

        TagValue(@NonNull byte[] source, int length) {
            data = Arrays.copyOf(source, length);
        }

        @NonNull
        byte[] data() {
            return data;
        }
    }
}
