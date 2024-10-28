import chevron

def render_template(template_path, data, output_path):
    # Read the Mustache template file
    with open(template_path, 'r') as template_file:
        template_content = template_file.read()

    # Read the data file (assuming it's in JSON format)
    # with open(data_path, 'r') as data_file:
    #     data_content = data_file.read()

    # Parse the data content into a dictionary
    # import json
    # data = json.loads(data_content)

    # Render the template with the data
    rendered_content = chevron.render(template_content, data)

    # Write the rendered content to the output file
    with open(output_path, 'w') as output_file:
        output_file.write(rendered_content)

if __name__ == "__main__":
    template_path = 'tasks/test.mustache'  # Path to your Mustache template file
    data = {'foo': 'Robin', 'bar': 'Batman'}             # Path to your JSON data file
    output_path = 'test_output.md'           # Path to your output file

    render_template(template_path, data, output_path)