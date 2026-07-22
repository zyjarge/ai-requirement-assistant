import { useEditor, EditorContent } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import Link from '@tiptap/extension-link';
import Placeholder from '@tiptap/extension-placeholder';
import { Button, Space, Tooltip } from 'antd';
import {
  BoldOutlined, ItalicOutlined, UnderlineOutlined,
  UnorderedListOutlined, OrderedListOutlined,
  LinkOutlined, UndoOutlined, RedoOutlined,
} from '@ant-design/icons';

interface RichEditorProps {
  value?: string;
  onChange?: (html: string) => void;
  placeholder?: string;
  readOnly?: boolean;
  minHeight?: number;
}

export default function RichEditor({ value, onChange, placeholder, readOnly, minHeight = 120 }: RichEditorProps) {
  const editor = useEditor({
    extensions: [
      StarterKit,
      Link.configure({ openOnClick: false }),
      Placeholder.configure({ placeholder: placeholder || '请输入...' }),
    ],
    content: value || '',
    editable: !readOnly,
    onUpdate: ({ editor }) => {
      onChange?.(editor.getHTML());
    },
  });

  if (!editor) return null;

  const setLink = () => {
    const url = window.prompt('请输入链接 URL', editor.getAttributes('link').href || '');
    if (url === null) return;
    if (url === '') {
      editor.chain().focus().extendMarkRange('link').unsetLink().run();
    } else {
      editor.chain().focus().extendMarkRange('link').setLink({ href: url }).run();
    }
  };

  return (
    <div style={{ border: '1px solid #d9d9d9', borderRadius: 6, background: '#fff' }}>
      {!readOnly && (
        <div style={{ borderBottom: '1px solid #f0f0f0', padding: 8 }}>
          <Space>
            <Tooltip title="加粗">
              <Button
                type="text"
                size="small"
                icon={<BoldOutlined />}
                onClick={() => editor.chain().focus().toggleBold().run()}
                className={editor.isActive('bold') ? 'ant-btn-active' : ''}
              />
            </Tooltip>
            <Tooltip title="斜体">
              <Button
                type="text"
                size="small"
                icon={<ItalicOutlined />}
                onClick={() => editor.chain().focus().toggleItalic().run()}
                className={editor.isActive('italic') ? 'ant-btn-active' : ''}
              />
            </Tooltip>
            <Tooltip title="代码">
              <Button
                type="text"
                size="small"
                icon={<UnderlineOutlined />}
                onClick={() => editor.chain().focus().toggleCode().run()}
                className={editor.isActive('code') ? 'ant-btn-active' : ''}
              />
            </Tooltip>
            <Tooltip title="无序列表">
              <Button
                type="text"
                size="small"
                icon={<UnorderedListOutlined />}
                onClick={() => editor.chain().focus().toggleBulletList().run()}
                className={editor.isActive('bulletList') ? 'ant-btn-active' : ''}
              />
            </Tooltip>
            <Tooltip title="有序列表">
              <Button
                type="text"
                size="small"
                icon={<OrderedListOutlined />}
                onClick={() => editor.chain().focus().toggleOrderedList().run()}
                className={editor.isActive('orderedList') ? 'ant-btn-active' : ''}
              />
            </Tooltip>
            <Tooltip title="链接">
              <Button type="text" size="small" icon={<LinkOutlined />} onClick={setLink} />
            </Tooltip>
            <Tooltip title="撤销">
              <Button type="text" size="small" icon={<UndoOutlined />} onClick={() => editor.chain().focus().undo().run()} />
            </Tooltip>
            <Tooltip title="重做">
              <Button type="text" size="small" icon={<RedoOutlined />} onClick={() => editor.chain().focus().redo().run()} />
            </Tooltip>
          </Space>
        </div>
      )}
      <EditorContent
        editor={editor}
        style={{
          padding: 12,
          minHeight,
          fontSize: 14,
          lineHeight: 1.6,
        }}
      />
    </div>
  );
}
